package org.kraftwerk28.spigot_tg_bridge

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Duration
import org.kraftwerk28.spigot_tg_bridge.Constants as C

typealias UpdateRequest = Call<TgApiService.TgResponse<List<TgApiService.Update>>>?

class TgBot(
    private val plugin: Plugin,
    private val config: Configuration,
    private val pollTimeout: Int = 30,
) {
    private val api: TgApiService
    private val client: OkHttpClient
    private val updateChan = Channel<TgApiService.Update>()
    private val scope = CoroutineScope(Dispatchers.Default)
    private val pollJob: Job
    private val handlerJob: Job
    private var currentOffset: Long = -1
    private var me: TgApiService.User
    private var commandRegex: Regex
    private val commandMap = config.commands.run {
        mapOf(
            online to ::onlineHandler,
            time to ::timeHandler,
            chatID to ::chatIdHandler,
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

        runBlocking {
            me = api.getMe().result!!
            // I intentionally don't put optional @username in regex
            // since bot is only used in group chats
            commandRegex = """^\/(\w+)(?:@${me.username})$""".toRegex()
            val commands = config.commands.run { listOf(time, online, chatID) }
                .zip(
                    C.COMMAND_DESC.run {
                        listOf(timeDesc, onlineDesc, chatIDDesc)
                    }
                )
                .map { TgApiService.BotCommand(it.first!!, it.second) }
                .let { TgApiService.SetMyCommands(it) }

            api.deleteWebhook(true)
            api.setMyCommands(commands)
        }

        pollJob = initPolling()
        handlerJob = initHandler()
    }

    private fun initPolling() = scope.launch {
        loop@ while (true) {
            try {
                api.getUpdates(offset = currentOffset, timeout = pollTimeout)
                    .result?.let { updates ->
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
    }

    private fun initHandler() = scope.launch {
        loop@ while (true) {
            try {
                handleUpdate(updateChan.receive())
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
    }

    suspend fun handleUpdate(update: TgApiService.Update) {
        // Ignore PM or channel
        if (listOf("private", "channel").contains(update.message?.chat?.type))
            return
        update.message?.text?.let {
            commandRegex.matchEntire(it)?.groupValues?.let {
                commandMap[it[1]]?.let { it(update) }
            } ?: run {
                onTextHandler(update)
            }
        }
    }

    fun stop() {
        runBlocking {
            pollJob.cancelAndJoin()
            handlerJob.cancelAndJoin()
        }
    }

    private suspend fun timeHandler(update: TgApiService.Update) {
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

    private suspend fun onlineHandler(update: TgApiService.Update) {
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

    private suspend fun chatIdHandler(update: TgApiService.Update) {
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

    private suspend fun onTextHandler(update: TgApiService.Update) {
        val msg = update.message!!
        if (!config.logFromTGtoMC || msg.from == null)
            return
        plugin.sendMessageToMinecraft(
            text = msg.text!!,
            username = msg.from.rawUserMention(),
            chatTitle = msg.chat.title,
        )
    }

    fun sendMessageToTelegram(
        text: String,
        username: String? = null,
        blocking: Boolean = false,
    ) {
        val formatted = username?.let {
            config.telegramFormat
                .replace(C.USERNAME_PLACEHOLDER, username.fullEscape())
                .replace(C.MESSAGE_TEXT_PLACEHOLDER, text.escapeHtml())
        } ?: text
        scope.launch {
            config.allowedChats.forEach { chatId ->
                api.sendMessage(chatId, formatted)
            }
        }.also {
            if (blocking) runBlocking { it.join() }
        }
    }
}
