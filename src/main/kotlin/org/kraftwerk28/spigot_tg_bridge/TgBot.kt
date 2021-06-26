package org.kraftwerk28.spigot_tg_bridge

import com.github.kotlintelegrambot.*
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.BotCommand
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.logging.LogLevel
import com.github.kotlintelegrambot.entities.ChatId
import okhttp3.logging.HttpLoggingInterceptor
import org.kraftwerk28.spigot_tg_bridge.Constants as C

class TgBot(private val plugin: Plugin, private val config: Configuration) {

    private lateinit var bot: Bot

    init {
        start(plugin, config)
    }

    fun start(plugin: Plugin, config: Configuration) {
        val slashRegex = "^/+".toRegex()
        val commands = config.commands

        skipUpdates()
        bot = bot {
            token = config.botToken
            logLevel = LogLevel.None

            val commandBindings = commands.let {
                mapOf(
                    it.time to ::time,
                    it.online to ::online,
                    it.chatID to ::chatID
                )
            }.filterKeys { it != null }

            dispatch {
                commandBindings.forEach { (text, handler) ->
                    command(text!!.replace(slashRegex, "")) {
                        handler(update)
                    }
                }
                text { onText(update) }
            }
        }
        bot.setMyCommands(getBotCommands())

        config.webhookConfig?.let { _ ->
            plugin.logger.info("Running in webhook mode.")
        } ?: run {
            bot.startPolling()
        }
    }

    fun stop() {
        bot.stopPolling()
    }

    private fun time(update: Update) {
        val msg = update.message!!
        if (!config.allowedChats.contains(msg.chat.id)) {
            return
        }

        if (plugin.server.worlds.isEmpty()) {
            bot.sendMessage(
                ChatId.fromId(msg.chat.id),
                "No worlds available",
                replyToMessageId = msg.messageId
            )
            return
        }

        val t = plugin.server.worlds.first().time
        val text = when {
            t <= 12000 -> C.TIMES_OF_DAY.day
            t <= 13800 -> C.TIMES_OF_DAY.sunset
            t <= 22200 -> C.TIMES_OF_DAY.night
            t <= 24000 -> C.TIMES_OF_DAY.sunrise
            else -> ""
        } + " ($t)"

        bot.sendMessage(
            ChatId.fromId(msg.chat.id),
            text,
            replyToMessageId = msg.messageId,
            parseMode = ParseMode.HTML
        )
    }

    private fun online(update: Update) {
        val msg = update.message!!
        if (!config.allowedChats.contains(msg.chat.id)) {
            return
        }

        val playerList = plugin.server.onlinePlayers
        val playerStr = plugin.server
            .onlinePlayers
            .mapIndexed { i, s -> "${i + 1}. ${fullEscape(s.displayName)}" }
            .joinToString("\n")
        val text =
            if (playerList.isNotEmpty()) "${config.onlineString}:\n$playerStr"
            else config.nobodyOnlineString
        bot.sendMessage(
            ChatId.fromId(msg.chat.id),
            text,
            replyToMessageId = msg.messageId,
            parseMode = ParseMode.HTML
        )
    }

    private fun chatID(update: Update) {
        val msg = update.message!!
        val chatID = msg.chat.id
        val text = """
            Chat ID:
            <code>$chatID</code>
            paste this id to <code>chats:</code> section in you config.yml file so it will look like this:
        """.trimIndent() +
                "\n\n<code>chats:\n  # other ids...\n  - ${chatID}</code>"
        bot.sendMessage(
            ChatId.fromId(chatID),
            text,
            parseMode = ParseMode.HTML,
            replyToMessageId = msg.messageId
        )
    }

    fun sendMessageToTelegram(text: String, username: String? = null) {
        config.allowedChats.forEach { chatID ->
            username?.let {
                bot.sendMessage(
                    ChatId.fromId(chatID),
                    formatMsgFromMinecraft(username, text),
                    parseMode = ParseMode.HTML,
                )
            } ?: run {
                bot.sendMessage(
                    ChatId.fromId(chatID),
                    text,
                    parseMode = ParseMode.HTML,
                )
            }
        }
    }

    private fun onText(update: Update) {
        if (!config.logFromTGtoMC) return
        val msg = update.message!!

        // Suppress commands to be sent to Minecraft
        if (msg.text!!.startsWith("/")) return

        plugin.sendMessageToMinecraft(msg.text!!, rawUserMention(msg.from!!))
    }

    private fun formatMsgFromMinecraft(
        username: String,
        text: String
    ): String =
        config.minecraftMessageFormat
            .replace("%username%", fullEscape(username))
            .replace("%message%", escapeHTML(text))

    private fun getBotCommands(): List<BotCommand> {
        val cmdList = config.commands.run { listOfNotNull(time, online, chatID) }
        val descList = C.COMMAND_DESC.run { listOf(timeDesc, onlineDesc, chatIDDesc) }
        return cmdList.zip(descList).map { BotCommand(it.first, it.second) }
    }

    private fun skipUpdates() {
        // Creates a temporary bot w/ 0 timeout to skip updates
        bot {
            token = config.botToken
            timeout = 0
            logLevel = LogLevel.None
        }.skipUpdates()
    }
}
