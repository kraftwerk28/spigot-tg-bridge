package org.kraftwerk28.spigot_tg_bridge

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.User
import okhttp3.logging.HttpLoggingInterceptor
import java.net.InetAddress
import org.kraftwerk28.spigot_tg_bridge.Constants as C

class TgBot(private val plugin: Plugin, private val config: Configuration) {

    private lateinit var bot: Bot

    init {
        start(plugin, config)
    }

    fun start(plugin: Plugin, config: Configuration) {
        val slashRegex = "^/+".toRegex()
        val commands = config.commands

        bot = bot {
            token = config.botToken
            logLevel = HttpLoggingInterceptor.Level.NONE
            dispatch {
                command(commands.time.replace(slashRegex, ""), ::time)
                command(commands.online.replace(slashRegex, ""), ::online)
                text(null, ::onText)
            }
        }
        skipUpdates()
        plugin.logger.info("Server address: ${InetAddress.getLocalHost().hostAddress}.")
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
        val playerList = plugin.server.onlinePlayers
        val playerStr = plugin.server
            .onlinePlayers
            .mapIndexed { i, s -> "${i + 1}. ${s.displayName}" }
            .joinToString("\n")
        val text =
            if (playerList.isNotEmpty()) "${config.onlineString}:\n$playerStr"
            else config.nobodyOnlineString
        val msg = update.message!!
        bot.sendMessage(
            msg.chat.id, text,
            replyToMessageId = msg.messageId,
            parseMode = ParseMode.HTML
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
                mcMessageStr(username, text),
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

    private fun mcMessageStr(username: String, text: String): String =
        "<b>${escapeHTML(username)}</b>: $text"

    private fun rawUserMention(user: User): String =
        (if (user.firstName.length < 2) null else user.firstName)
            ?: user.username
            ?: user.lastName!!

    private fun skipUpdates(lastUpdateID: Long = 0) {
        val newUpdates = bot.getUpdates(lastUpdateID)

        if (newUpdates.isNotEmpty()) {
            val lastUpd = newUpdates.last()
            if (lastUpd !is Update) return
            return skipUpdates(lastUpd.updateId + 1)
        }
    }

    companion object {
        fun escapeHTML(s: String): String =
            s.replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;")
    }
}