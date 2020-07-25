package org.kraftwerk28.spigot_tg_bridge

import com.github.kotlintelegrambot.*
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.BotCommand
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.User
import okhttp3.logging.HttpLoggingInterceptor
import org.kraftwerk28.spigot_tg_bridge.Constants as C

fun Bot.skipUpdates(lastUpdateID: Long = 0) {
    val newUpdates = getUpdates(lastUpdateID)

    if (newUpdates.isNotEmpty()) {
        val lastUpd = newUpdates.last()
        if (lastUpd !is Update) return
        return skipUpdates(lastUpd.updateId + 1)
    }
}

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
            logLevel = HttpLoggingInterceptor.Level.NONE

            val cmdBinding = commands.let {
                mapOf(
                    it.time to ::time,
                    it.online to ::online,
                    it.chatID to ::chatID
                )
            }.filterKeys { it != null }

            dispatch {
                cmdBinding.forEach { (text, handler) ->
                    command(text!!.replace(slashRegex, ""), handler)
                }
                text(null, ::onText)
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

    private fun time(bot: Bot, update: Update) {
        val msg = update.message!!
        if (!config.allowedChats.contains(msg.chat.id)) {
            return
        }

        if (plugin.server.worlds.isEmpty()) {
            bot.sendMessage(
                msg.chat.id,
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
            msg.chat.id, text,
            replyToMessageId = msg.messageId,
            parseMode = ParseMode.HTML
        )
    }

    private fun online(bot: Bot, update: Update) {
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
            msg.chat.id, text,
            replyToMessageId = msg.messageId,
            parseMode = ParseMode.HTML
        )
    }

    private fun chatID(bot: Bot, update: Update) {
        val msg = update.message!!
        val chatID = msg.chat.id
        val text = """
            Chat ID:
            <code>$chatID</code>
            paste this id to <code>chats:</code> section in you config.yml file so it will look like this:
        """.trimIndent() +
                "\n\n<code>chats:\n  # other ids...\n  - ${chatID}</code>"
        bot.sendMessage(
            chatID,
            text,
            parseMode = ParseMode.HTML,
            replyToMessageId = msg.messageId
        )
    }

    fun broadcastToTG(text: String) {
        config.allowedChats.forEach { chatID ->
            bot.sendMessage(chatID, text, parseMode = ParseMode.HTML)
        }
    }

    fun sendMessageToTGFrom(username: String, text: String) {
        config.allowedChats.forEach { chatID ->
            bot.sendMessage(
                chatID,
                messageFromMinecraft(username, text),
                parseMode = ParseMode.HTML
            )
        }
    }

    private fun onText(bot: Bot, update: Update) {
        if (!config.logFromTGtoMC) return
        val msg = update.message!!
        if (msg.text!!.startsWith("/")) return // Suppress command forwarding
        plugin.sendMessageToMCFrom(rawUserMention(msg.from!!), msg.text!!)
    }

    private fun messageFromMinecraft(username: String, text: String): String =
        config.minecraftMessageFormat
            .replace("%username%", fullEscape(username))
            .replace("%message%", escapeHTML(text))

    private fun rawUserMention(user: User): String =
        (if (user.firstName.length < 2) null else user.firstName)
            ?: user.username
            ?: user.lastName!!

    private fun getBotCommands(): List<BotCommand> {
        val cmdList = config.commands.run { listOfNotNull(time, online, chatID) }
        val descList = C.COMMAND_DESC.run { listOf(timeDesc, onlineDesc, chatIDDesc) }
        return cmdList.zip(descList).map { BotCommand(it.first, it.second) }
    }

    private fun skipUpdates() {
        bot {
            token = config.botToken
            timeout = 0
            logLevel = HttpLoggingInterceptor.Level.NONE
        }.skipUpdates()
    }

    companion object {
        fun escapeHTML(s: String) = s
            .replace("&", "&amp;")
            .replace(">", "&gt;")
            .replace("<", "&lt;")

        fun escapeColorCodes(s: String) = s.replace("\u00A7.".toRegex(), "")

        fun fullEscape(s: String) = escapeColorCodes(escapeHTML(s))
    }
}