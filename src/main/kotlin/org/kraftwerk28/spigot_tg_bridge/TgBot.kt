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

class TgBot(val plugin: Plugin) {

    private val commands = Commands(plugin)
    private val bot: Bot
    private val allowedChats: List<Long>
    private val chatToMC: Boolean
    private val botToken: String
    private val botUsername: String

    init {
        plugin.config.run {
            allowedChats = getLongList(Constants.FIELDS.ALLOWED_CHATS)
            chatToMC = getBoolean(Constants.FIELDS.LOG_FROM_TG_TO_MC, Constants.DEFS.logFromTGtoMC)
            botToken = getString(Constants.FIELDS.BOT_TOKEN) ?: throw Exception(Constants.WARN.noToken)
            botUsername = getString(Constants.FIELDS.BOT_USERNAME) ?: throw Exception(Constants.WARN.noUsername)
        }
        val slashRegex = "^/+".toRegex()

        bot = bot {
            token = botToken
            logLevel = HttpLoggingInterceptor.Level.NONE
            dispatch {
                text(null, ::onText)
                command(commands.time.replace(slashRegex, ""), ::time)
                command(commands.online.replace(slashRegex, ""), ::online)
            }
        }
        bot.startPolling()
    }

    private fun time(bot: Bot, update: Update) {
        val t = plugin.server.worlds[0].time
        var text = when {
            t <= 12000 -> "\uD83C\uDFDE Day"
            t <= 13800 -> "\uD83C\uDF06 Sunset"
            t <= 22200 -> "\uD83C\uDF03 Night"
            t <= 24000 -> "\uD83C\uDF05 Sunrise"
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
            Constants.FIELDS.STRINGS.ONLINE,
            Constants.DEFS.playersOnline
        )!!
        val offlineStr = plugin.config.getString(
            Constants.FIELDS.STRINGS.OFFLINE,
            Constants.DEFS.nobodyOnline
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