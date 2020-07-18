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

class TgBot(val plugin: Plugin) {

    private val commands = Commands(plugin)
    private val bot: Bot
    private val allowedChats: List<Long>
    private val chatToMC: Boolean
    private val botToken: String
    private val botUsername: String
    private val allowWebhook: Boolean
    private var webhookConfig: Map<String, Any>? = null

    init {
        plugin.config.run {
            allowedChats = getLongList(C.FIELDS.ALLOWED_CHATS)
            chatToMC = getBoolean(C.FIELDS.LOG_FROM_TG_TO_MC, C.DEFS.logFromTGtoMC)
            botToken = getString(C.FIELDS.BOT_TOKEN) ?: throw Exception(C.WARN.noToken)
            botUsername = getString(C.FIELDS.BOT_USERNAME) ?: throw Exception(C.WARN.noUsername)
            allowWebhook = getBoolean(C.FIELDS.USE_WEBHOOK, C.DEFS.useWebhook)

            val whCfg = get(C.FIELDS.WEBHOOK_CONFIG)
            if (whCfg is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                webhookConfig = whCfg as Map<String, Any>?
            }
        }
        val slashRegex = "^/+".toRegex()

        bot = bot {
            token = botToken
            logLevel = HttpLoggingInterceptor.Level.NONE
            dispatch {
                command(commands.time.replace(slashRegex, ""), ::time)
                command(commands.online.replace(slashRegex, ""), ::online)
                text(null, ::onText)
            }
        }
        plugin.logger.info("Server address: ${InetAddress.getLocalHost().hostAddress}.")
        webhookConfig?.let { config ->
            plugin.logger.info("Running in webhook mode.")
        } ?: run {
            bot.startPolling()
        }
    }

    private fun time(bot: Bot, update: Update) {
        val t = plugin.server.worlds[0].time
        var text = when {
            t <= 12000 -> C.TIMES_OF_DAY.day
            t <= 13800 -> C.TIMES_OF_DAY.sunset
            t <= 22200 -> C.TIMES_OF_DAY.night
            t <= 24000 -> C.TIMES_OF_DAY.sunrise
            else -> ""
        }
        text += " ($t)"
        val msg = update.message!!
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
        val onlineStr = plugin.config.getString(
            C.FIELDS.STRINGS.ONLINE,
            C.DEFS.playersOnline
        )!!
        val offlineStr = plugin.config.getString(
            C.FIELDS.STRINGS.OFFLINE,
            C.DEFS.nobodyOnline
        )!!
        val text =
            if (playerList.isNotEmpty()) "$onlineStr:\n$playerStr"
            else offlineStr
        val msg = update.message!!
        bot.sendMessage(
            msg.chat.id, text,
            replyToMessageId = msg.messageId,
            parseMode = ParseMode.HTML
        )
    }

    fun broadcastToTG(text: String) {
        allowedChats.forEach { chatID ->
            bot.sendMessage(chatID, text, parseMode = ParseMode.HTML)
        }
    }

    fun sendMessageToTGFrom(username: String, text: String) {
        allowedChats.forEach { chatID ->
            bot.sendMessage(
                chatID,
                mcMessageStr(username, text),
                parseMode = ParseMode.HTML
            )
        }
    }

    private fun onText(bot: Bot, update: Update) {
        if (!chatToMC) return
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

    companion object {
        fun escapeHTML(s: String): String =
            s.replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;")
    }
}