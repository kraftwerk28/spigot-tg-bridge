package org.kraftwerk28.spigot_tg_bridge

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Duration
import org.kraftwerk28.spigot_tg_bridge.Constants as C

typealias UpdateRequest = Call<TgResponse<List<Update>>>?

class TgBot(
    private val plugin: Plugin,
    private val config: Configuration,
) {
    private val api: TgApiService
    private val client: OkHttpClient
    private val updateChan = Channel<Update>()
    private var pollJob: Job? = null
    private var handlerJob: Job? = null
    private var currentOffset: Long = -1
    private var me: User? = null
    private var commandRegex: Regex? = null
    private val commandMap: Map<String?, suspend (u: Update) -> Unit> =
        config.commands.run {
            mapOf(
                online to ::onlineHandler,
                time to ::timeHandler,
                chatID to ::chatIdHandler,
                linkIgn to ::linkIgnHandler,
                getAllLinked to ::getLinkedUsersHandler,
            )
        }

    init {
        client = OkHttpClient
            .Builder()
            .readTimeout(Duration.ZERO)
            .build()
        api = Retrofit.Builder()
            .baseUrl("https://api.telegram.org/bot${config.botToken}/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TgApiService::class.java)
    }

    private suspend fun initialize() {
        me = api.getMe().result!!
        // I intentionally don't put optional @username in regex
        // since bot is only used in group chats
        commandRegex = """^\/(\w+)(?:@${me!!.username})$""".toRegex()
        val commands = config.commands.run { listOf(time, online, chatID) }
            .zip(
                C.COMMAND_DESC.run {
                    listOf(timeDesc, onlineDesc, chatIDDesc)
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
        loop@ while (true) {
            try {
                api.getUpdates(
                    offset = currentOffset,
                    timeout = config.pollTimeout,
                ).result?.let { updates ->
                    if (!updates.isEmpty()) {
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
        updateChan.close()
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

    suspend fun handleUpdate(update: Update) {
        // Ignore PM or channel
        if (listOf("private", "channel").contains(update.message?.chat?.type))
            return
        update.message?.text?.let {
            commandRegex?.matchEntire(it)?.groupValues?.let {
                commandMap.get(it[1])?.let { it(update) }
            } ?: run {
                onTextHandler(update)
            }
        }
    }

    private suspend fun timeHandler(update: Update) {
        val msg = update.message!!
        if (!config.allowedChats.contains(msg.chat.id)) {
            return
        }
        if (plugin.server.worlds.isEmpty()) {
            api.sendMessage(
                msg.chat.id,
                "No worlds available",
                replyToMessageId = msg.messageId
            )
            return
        }
        // TODO: handle multiple worlds
        val time = plugin.server.worlds.first().time
        val text = C.TIMES_OF_DAY.run {
            when {
                time <= 12000 -> day
                time <= 13800 -> sunset
                time <= 22200 -> night
                time <= 24000 -> sunrise
                else -> ""
            }
        } + " ($time)"
        api.sendMessage(msg.chat.id, text, replyToMessageId = msg.messageId)
    }

    private suspend fun onlineHandler(update: Update) {
        val msg = update.message!!
        if (!config.allowedChats.contains(msg.chat.id)) {
            return
        }
        val playerList = plugin.server.onlinePlayers
        val playerStr = plugin.server
            .onlinePlayers
            .mapIndexed { i, s -> "${i + 1}. ${s.displayName.fullEscape()}" }
            .joinToString("\n")
        val text =
            if (playerList.isNotEmpty()) "${config.onlineString}:\n$playerStr"
            else config.nobodyOnlineString
        api.sendMessage(msg.chat.id, text, replyToMessageId = msg.messageId)
    }

    private suspend fun chatIdHandler(update: Update) {
        val msg = update.message!!
        val chatId = msg.chat.id
        val text = """
        |Chat ID: <code>$chatId</code>.
        |Copy this id to <code>chats</code> section in your <b>config.yml</b> file so it will look like this:
        |
        |<pre>chats:
        |  # other ids...
        |  - $chatId</pre>
        """.trimMargin()
        api.sendMessage(chatId, text, replyToMessageId = msg.messageId)
    }

    private suspend fun linkIgnHandler(update: Update) {
        val tgUser = update.message!!.from!!
        val mcUuid = getMinecraftUuidByUsername(update.message.text!!)
        if (mcUuid == null) {
            // Respond...
            return
        }
        val linked = plugin.ignAuth?.linkUser(
            tgId = tgUser.id,
            tgFirstName = tgUser.firstName,
            tgLastName = tgUser.lastName,
            minecraftUsername = tgUser.username,
            minecraftUuid = mcUuid,
        )
        println(tgUser.toString())
    }

    private suspend fun onTextHandler(update: Update) {
        val msg = update.message!!
        if (!config.logFromTGtoMC || msg.from == null)
            return
        plugin.sendMessageToMinecraft(
            text = msg.text!!,
            username = msg.from.rawUserMention(),
            chatTitle = msg.chat.title,
        )
    }

    private suspend fun getLinkedUsersHandler(update: Update) {
        val linkedUsers = plugin.ignAuth?.run {
            getAllLinkedUsers()
        } ?: listOf()
        if (linkedUsers.isEmpty()) {
            api.sendMessage(update.message!!.chat.id, "No linked users.")
        } else {
            val text = "<b>Linked users:</b>\n" +
                linkedUsers.mapIndexed { i, dbUser ->
                    "${i + 1}. ${dbUser.fullName()}"
                }.joinToString("\n")
            api.sendMessage(update.message!!.chat.id, text)
        }
    }

    suspend fun sendMessageToTelegram(text: String, username: String? = null) {
        val formatted = username?.let {
            config.telegramFormat
                .replace(C.USERNAME_PLACEHOLDER, username.fullEscape())
                .replace(C.MESSAGE_TEXT_PLACEHOLDER, text.escapeHtml())
        } ?: text
        config.allowedChats.forEach { chatId ->
            api.sendMessage(chatId, formatted)
        }
        // plugin.launch {
        //     config.allowedChats.forEach { chatId ->
        //         api.sendMessage(chatId, formatted)
        //     }
        // }
    }
}
