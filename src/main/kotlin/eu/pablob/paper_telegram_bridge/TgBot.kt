package eu.pablob.paper_telegram_bridge

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Duration
import eu.pablob.paper_telegram_bridge.Constants as C
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import retrofit2.HttpException


typealias CmdHandler = suspend (HandlerContext) -> Unit

data class HandlerContext(
    val update: Update,
    val message: Message?,
    val chat: Chat?,
    val commandArgs: List<String> = listOf(),
)

class TgBot(
    private val plugin: Plugin,
    private val config: Configuration,
) {
    private val client: OkHttpClient = OkHttpClient
        .Builder()
        // Disable timeout to make long-polling possible
        .readTimeout(Duration.ZERO)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level =
                    if (config.debugHttp) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
            }
        )
        .build()
    private val api = Retrofit.Builder()
        .baseUrl("${config.apiOrigin}/bot${config.botToken}/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TgApiService::class.java)
    private val updateChan = Channel<Update>()
    private var pollJob: Job? = null
    private var handlerJob: Job? = null
    private var currentOffset: Long = -1
    private var me: User? = null
    private var commandRegex: Regex? = null
    private val commandMap: Map<String?, CmdHandler> = config.commands.run {
        mapOf(
            online to ::onlineHandler,
            time to ::timeHandler,
            chatID to ::chatIdHandler,
            whitelist to ::whitelistHandler
        )
    }


    private suspend fun initialize() {
        me = api.getMe().result!!
        // Make @username optional so commands work without bot tag in groups
        commandRegex = """^/(\w+)(?:@${me!!.username})?(?:\s+(.+))?$""".toRegex()
        val commands = config.commands.run { listOf(time, online, chatID, whitelist) }
            .zip(
                C.CommandDesc.run {
                    listOf(TIME_DESC, ONLINE_DESC, CHAT_ID_DESC, WHITELIST_DESC)
                }
            )
            .map { BotCommand(it.first!!, it.second) }
            .let { SetMyCommands(it) }
        api.deleteWebhook(dropPendingUpdates = true)
        api.setMyCommands(commands)
    }

    suspend fun startPolling() {
        initialize()
        pollJob = initPolling()
        handlerJob = initHandler()
    }

    suspend fun stop() {
        pollJob?.cancelAndJoin()
        handlerJob?.join()
    }

    private fun initPolling() = plugin.launch {
        try {
            loop@ while (true) {
                try {
                    api.getUpdates(
                        offset = currentOffset,
                        timeout = config.pollTimeout,
                    ).result?.let { updates ->
                        if (updates.isNotEmpty()) {
                            updates.forEach { updateChan.send(it) }
                            currentOffset = updates.last().updateId + 1
                        }
                    }
                } catch (e: Exception) {
                    when (e) {
                        is CancellationException -> break@loop
                        else -> {
                            e.printStackTrace()
                            continue@loop
                        }
                    }
                }
            }
        } finally {
            updateChan.close()
        }
    }

    private fun initHandler() = plugin.launch {
        updateChan.consumeEach {
            try {
                handleUpdate(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun handleUpdate(update: Update) {


        // Handle callback queries
        if (update.callbackQuery != null) {
            handleCallbackQuery(update.callbackQuery)
            return
        }
        // Accept only these chat types
        val message = update.message ?: return
        if (message.chat.type !in listOf("group", "supergroup")) return

        val topicId = message.messageThreadId.takeIf { message.isTopicMessage == true } ?: 1L
        val allowedTopics = config.allowedTopics.ifEmpty { listOf(1L) }
        if (topicId !in allowedTopics) return

        // Check whether the chat is white-listed in config
        if (message.chat.id !in config.allowedChats) return

        val ctx = HandlerContext(update, message, message.chat)
        val mediaLabel = when {
            message.photo != null -> config.mediaMessagePhotoString
            message.video != null -> config.mediaMessageVideoString
            message.audio != null -> config.mediaMessageAudioString
            message.voice != null -> config.mediaMessageVoiceString
            message.videoNote != null -> config.mediaMessageVideoNoteString
            else -> null
        }
        if (mediaLabel != null) {
            val chatId = message.chat.id.toString().removePrefix("-100")
            // link to the message.
            val link = "https://t.me/c/$chatId/${message.messageId}"
            val caption = message.caption
                ?.takeIf { it.isNotBlank() }
                ?.let { " $it" }
                .orEmpty()
            onTextHandler(
                ctx,
                "$mediaLabel$caption",
                clickableUrl = link,
                clickableText = mediaLabel,
                escapeMessageEmoji = false,
            )
            return
        }

        val text = message.text ?: message.caption ?: return
        val commandMatch = message.text?.let { commandRegex?.matchEntire(it) }
        val commandHandler = commandMatch?.groupValues?.let { commandMap[it[1]] }
        if (commandMatch != null && commandHandler != null) {
            commandHandler(
                ctx.copy(commandArgs = commandMatch.groupValues[2].split("""\s+""".toRegex()))
            )
            return
        }
        onTextHandler(ctx, text)
    }

    private suspend fun timeHandler(ctx: HandlerContext) {
        val msg = ctx.message!!
        if (!config.allowedChats.contains(msg.chat.id)) {
            return
        }
        if (plugin.server.worlds.isEmpty()) {
            api.sendMessage(
                msg.chat.id,
                "No worlds available",
                replyToMessageId = msg.messageId,
                messageThreadId = msg.messageThreadId
            )
            return
        }
        // TODO: handle multiple worlds
        val time = plugin.server.worlds.first().time
        val text = C.TimesOfDay.run {
            when {
                time <= 12000 -> DAY
                time <= 13800 -> SUNSET
                time <= 22200 -> NIGHT
                time <= 24000 -> SUNRISE
                else -> ""
            }
        } + " ($time)"
        api.sendMessage(
            msg.chat.id,
            text,
            replyToMessageId = msg.messageId,
            messageThreadId = msg.messageThreadId
        )
    }

    private suspend fun onlineHandler(ctx: HandlerContext) {
        val msg = ctx.message!!
        if (!config.allowedChats.contains(msg.chat.id)) {
            return
        }
        // Filter out players with the 'tg-bridge.silentjoinleave' permission
        val visiblePlayers = plugin.server.onlinePlayers.filter { !it.hasPermission("tg-bridge.silentjoinleave") }
        val playerStr = visiblePlayers
            .mapIndexed { i, s -> "${i + 1}. ${s.playerProfile.name?.fullEscape()}" }
            .joinToString("\n")
        val text =
            if (visiblePlayers.isNotEmpty()) "${config.onlineString}:\n$playerStr"
            else config.nobodyOnlineString
        api.sendMessage(
            msg.chat.id,
            text,
            replyToMessageId = msg.messageId,
            messageThreadId = msg.messageThreadId
        )
    }

    private suspend fun chatIdHandler(ctx: HandlerContext) {
        val msg = ctx.message!!
        val chatId = msg.chat.id
        val text = """
        |Chat ID: <code>$chatId</code>.
        |Copy this id to <code>chats</code> section in your <b>config.yml</b> file so it looks like this:
        |<pre>
        |chats: [
        |  $chatId,
        |  # other chat ids...
        |]
        |</pre>
        """.trimMargin()
        api.sendMessage(
            chatId,
            text,
            replyToMessageId = msg.messageId,
            messageThreadId = msg.messageThreadId
        )
    }

    private fun onTextHandler(
        ctx: HandlerContext,
        text: String,
        clickableUrl: String? = null,
        clickableText: String? = null,
        escapeMessageEmoji: Boolean = config.escapeEmojis,
    ) {
        val msg = ctx.message!!
        if (!config.logFromTGtoMC || msg.from == null)
            return
        plugin.sendMessageToMinecraft(
            text = text,
            username = msg.from.rawUserMention(),
            chatTitle = msg.chat.title,
            clickableUrl = clickableUrl,
            clickableText = clickableText,
            escapeMessageEmoji = escapeMessageEmoji,
        )
    }

    suspend fun sendMessageToTelegram(text: String, username: String? = null) {
        val formatted = username?.let {
            config.telegramFormat
                .replace(C.USERNAME_PLACEHOLDER, username.fullEscape())
                .replace(C.MESSAGE_TEXT_PLACEHOLDER, text.escapeHtml())
        } ?: text
        telegramDestinations().forEach { (chatId, topicId) ->
            try {
                api.sendMessage(
                    chatId,
                    formatted,
                    messageThreadId = topicId,
                    disableNotification = config.silentMessages
                )
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                plugin.logger.warning(
                    "Telegram sendMessage failed for chat $chatId" +
                            (topicId?.let { ", topic $it" } ?: "") +
                            ": ${errorBody ?: e.message()}"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun whitelistHandler(ctx: HandlerContext) {
        val msg = ctx.message!!
        if (!config.admins.contains(msg.from?.id)) {
            return
        }
        val wl = Whitelist(plugin)
        var text: String
        val parts = msg.text!!.split(" ")
        if (parts.size <= 1) {
            // No player name provided, list all whitelisted players
            text = wl.getWhitelistedNames().toString()
        } else {
            // Player name provided, toggle their whitelist status
            val playerName = parts[1]
            val gotAdded = wl.toggleWhitelist(playerName)
            text =
                "${if (gotAdded) "\uD83D\uDFE9" else "\uD83D\uDFE5"} $playerName got ${if (gotAdded) "added to" else "removed from"} the whitelist."
        }
        api.sendMessage(
            msg.chat.id,
            text,
            replyToMessageId = msg.messageId,
            messageThreadId = msg.messageThreadId
        )

    }

    suspend fun sendPhotoToTelegram(imageBytes: ByteArray, caption: String) {
        val requestBody = imageBytes.toRequestBody("image/png".toMediaTypeOrNull())
        val photoPart = MultipartBody.Part.createFormData("photo", "image.png", requestBody)
        val text = caption.toRequestBody("text/plain".toMediaTypeOrNull())

        telegramDestinations().forEach { (chatId, topicId) ->
            try {
                api.sendPhoto(
                    chatId = chatId,
                    photo = photoPart,
                    caption = text,
                    messageThreadId = topicId,
                    disableNotification = config.silentMessages
                )
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                plugin.logger.warning("Telegram API error: $errorBody")
            }
        }
    }


    private fun createInlineKeyboardJson(
        prevCallbackData: String,
        nextCallbackData: String,
        isFirstPage: Boolean,
        isLastPage: Boolean
    ): String {
        if (isFirstPage) {
            return JSONObject().apply {
                put(
                    "inline_keyboard", listOf(
                        listOf(
                            JSONObject().apply {
                                put("text", "Next ➡️")
                                put("callback_data", nextCallbackData)
                            }
                        )
                    ))
            }.toString()
        } else if (isLastPage) {
            return JSONObject().apply {
                put(
                    "inline_keyboard", listOf(
                        listOf(
                            JSONObject().apply {
                                put("text", "⬅️ Back")
                                put("callback_data", prevCallbackData)
                            }
                        )
                    ))
            }.toString()
        }
        return JSONObject().apply {
            put(
                "inline_keyboard", listOf(
                    listOf(
                        JSONObject().apply {
                            put("text", "⬅️ Back")
                            put("callback_data", prevCallbackData)
                        },
                        JSONObject().apply {
                            put("text", "Next ➡️")
                            put("callback_data", nextCallbackData)
                        }
                    )
                ))
        }.toString()
    }


    suspend fun sendImageWithKeyboard(imageIndex: Int, imageDirectory: File, caption: String?) {
        val imageFile = File(imageDirectory, "page$imageIndex.png")
        val totalPages = imageDirectory.listFiles { file -> file.name.endsWith(".png") }?.size ?: 1
        val isLastPage = imageIndex == totalPages

        val requestBody = imageFile.asRequestBody("image/png".toMediaTypeOrNull())
        val photoPart = MultipartBody.Part.createFormData("photo", imageFile.name, requestBody)

        val bookHash = imageDirectory.toString().split("/")[4]

        // Manually create the JSON string for the inline keyboard
        var keyboardJson = ""
        if (!isLastPage) {
            keyboardJson = createInlineKeyboardJson(
                "prev_$imageIndex-$bookHash", "next_$imageIndex-$bookHash",
                isFirstPage = true,
                isLastPage = false
            )
        }
        // Convert the JSON string to a RequestBody
        val replyMarkupBody = keyboardJson.toRequestBody("application/json".toMediaTypeOrNull())

        telegramDestinations().forEach { (chatId, topicId) ->
            try {
                api.sendPhoto(
                    chatId = chatId,
                    photo = photoPart,
                    replyMarkup = replyMarkupBody, // Pass the JSON as a RequestBody
                    caption = caption?.toRequestBody("text/plain".toMediaTypeOrNull()),
                    messageThreadId = topicId,
                    disableNotification = config.silentMessages
                )
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                plugin.logger.warning("Telegram API error: $errorBody")
            }
        }
    }

    private fun telegramDestinations(): List<Pair<Long, Long?>> {
        val topicIds = config.allowedTopics.ifEmpty { listOf<Long?>(null) }
        return config.allowedChats.flatMap { chatId ->
            topicIds.map { topicId -> chatId to topicId }
        }
    }

    suspend fun editImageWithKeyboard(
        chatId: Long,
        messageId: Long,
        imageIndex: Int,
        imageDirectory: File,
        hash: String
    ) {
        val imageFile = File(imageDirectory, "page$imageIndex.png")
        val totalPages = imageDirectory.listFiles { file -> file.name.endsWith(".png") }?.size ?: 1

        val isFirstPage = imageIndex == 1
        val isLastPage = imageIndex == totalPages

        val requestBody = imageFile.asRequestBody("image/png".toMediaTypeOrNull())
        val photoPart = MultipartBody.Part.createFormData("media", imageFile.name, requestBody)

        val keyboardJson =
            createInlineKeyboardJson("prev_$imageIndex-$hash", "next_$imageIndex-$hash", isFirstPage, isLastPage)
        val replyMarkupBody = keyboardJson.toRequestBody("application/json".toMediaTypeOrNull())

        val mediaJson = """
        {
            "type": "photo",
            "media": "attach://media"
        }
    """.trimIndent().toRequestBody("application/json".toMediaTypeOrNull())

        try {
            api.editMessageMedia(
                chatId = chatId,
                messageId = messageId,
                media = mediaJson,
                photo = photoPart,
                replyMarkup = replyMarkupBody
            )
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            plugin.logger.warning("Telegram API error: $errorBody")
        }
    }


    private suspend fun handleCallbackQuery(callbackQuery: CallbackQuery) {
        val message = callbackQuery.message ?: return
        val chatId = message.chat.id
        val messageId = message.messageId
        val data = callbackQuery.data ?: return

        var currentImageIndex = data.substringAfter("_").substringBefore("-").toInt()
        val action = data.substringBefore("_")
        val hash = data.substringAfter("-")

        when (action) {
            "prev" -> currentImageIndex -= 1
            "next" -> currentImageIndex += 1
        }

        // Edit the existing message instead of sending a new one
        editImageWithKeyboard(chatId, messageId, currentImageIndex, File(plugin.dataFolder, "inv/books/$hash"), hash)

        // Acknowledge the callback query
        api.answerCallbackQuery(callbackQuery.id)
    }


}
