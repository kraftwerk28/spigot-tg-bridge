package org.kraftwerk28.spigot_tg_bridge

import org.kraftwerk28.spigot_tg_bridge.Constants as C
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

class TgBot(
    private val plugin: Plugin,
    private val config: Configuration,
    private val pollTimeout: Int = 30,
) {
    private val api: TgApiService
    val updateChan = Channel<TgApiService.Update>()
    val scope = CoroutineScope(Dispatchers.Default)
    val pollJob: Job
    val handlerJob: Job
    var currentOffset: Long = -1
    var me: TgApiService.User
    var commandRegex: Regex
    val commandMap = config.commands.run {
        mapOf(
            online to ::onlineHandler,
            time to ::timeHandler,
            chatID to ::chatIdHandler,
        )
    }

    init {
        api = TgApiService.create(config.botToken)
        runBlocking {
            me = api.getMe().result!!
            // I don't put optional @username in regex since bot is
            // only used in group chats
            commandRegex = """^\/(\w+)(?:@${me.username})$""".toRegex()


            val commands = config.commands.run { listOf(time, online, chatID) }
                .zip(C.COMMAND_DESC.run {
                        listOf(timeDesc, onlineDesc, chatIDDesc)
                })
                .map { TgApiService.BotCommand(it.first!!, it.second) }
                .let { TgApiService.SetMyCommands(it) }

            api.setMyCommands(commands)
        }
        pollJob = scope.launch {
            try {
                while (true) {
                    try {
                        pollUpdates()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: CancellationException) {}
        }
        handlerJob = scope.launch {
            try {
                while (true) {
                    handleUpdate()
                }
            } catch (e: CancellationException) {}
        }
    }

    suspend fun pollUpdates() {
        val updatesResponse = api
            .getUpdates(offset = currentOffset, timeout = pollTimeout)
        updatesResponse.result?.let { updates ->
            if (!updates.isEmpty()) {
                updates.forEach { updateChan.send(it) }
                currentOffset = updates.last().updateId + 1
            }
        }
    }

    suspend fun handleUpdate() {
        val update = updateChan.receive()
        update.message?.text?.let {
            println("Text: $it")
            commandRegex.matchEntire(it)?.groupValues?.let {
                commandMap[it[1]]?.let { it(update) } ?: onTextHandler(update)
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
            Chat ID:
            <code>${chatId}</code>
            paste this id to <code>chats:</code> section in you config.yml file so it will look like this:
        """.trimIndent() +
                "\n\n<code>chats:\n  # other ids...\n  - ${chatId}</code>"
        api.sendMessage(chatId, text, replyToMessageId = msg.messageId)
    }

    fun sendMessageToTelegram(text: String, username: String? = null) {
        val messageText = username?.let { formatMsgFromMinecraft(it, text) } ?: text
        config.allowedChats.forEach { chatId ->
            scope.launch {
                delay(1000)
                api.sendMessage(chatId, messageText)
            }
        }
    }

    private suspend fun onTextHandler(update: TgApiService.Update) {
        if (!config.logFromTGtoMC) return
        val msg = update.message!!
        plugin.sendMessageToMinecraft(msg.text!!, msg.from!!.rawUserMention())
    }

    private fun formatMsgFromMinecraft(
        username: String,
        text: String
    ): String =
        config.minecraftMessageFormat
            .replace("%username%", username.fullEscape())
            .replace("%message%", text.escapeHtml())
}
