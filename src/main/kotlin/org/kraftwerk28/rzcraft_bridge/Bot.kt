package org.kraftwerk28.rzcraft_bridge

import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

class Bot(plugin: Plugin) : TelegramLongPollingBot() {
    private var plugin: Plugin
    private var allowedChats: List<Long> = listOf()
    private var chatToMC: Boolean = false
    private var botToken: String = ""
    private var botUsername: String = ""

    init {
        this.plugin = plugin
        chatToMC = plugin.config.getBoolean("logFromTGtoMC", false)
        allowedChats = plugin.config.getLongList("chats")
        botToken = plugin.config.getString("botToken")!!
        botUsername = plugin.config.getString("botUsername")!!
    }

    override fun getBotToken() = botToken

    override fun getBotUsername() = botUsername

    override fun onUpdateReceived(update: Update?) {
        val msg = update?.message
        if (msg == null || msg.text == null) return
        val allowedChats = plugin.config.getLongList("chats")
        if (!allowedChats.contains(msg.chatId)) return

        // cmd shows online players
        if (msg.text.startsWith("/online")) {
            val playerCount = plugin.server.onlinePlayers.size
//            val playerList = plugin.server
//                .getOnlinePlayers()
//                .mapIndexed { i, s -> "${i + 1}. ${s.displayName}" }
//                .joinToString("\n")
            val onlineStr = plugin.config.getString("strings.online", "Online")
            val offlineStr = plugin.config.getString(
                "strings.nobodyOnline",
                "Nobody online"
            )
            val text =
                if (playerCount > 0) "$onlineStr: $playerCount"
                else offlineStr
            execute(SendMessage(msg.chatId, text).setParseMode(ParseMode.HTML))
        }
        // stop, if no command matched:
        if (msg.text!!.startsWith("/")) return

        if (chatToMC)
            plugin.sendMessageToMCFrom(rawUserMention(msg.from), msg.text)
    }

    fun sendMessageToTGFrom(username: String, text: String) {
        allowedChats.forEach {
            try {
                val msg = SendMessage(it, mcMessageStr(username, text))
                    .setParseMode(ParseMode.HTML)
                execute(msg)
            } catch (e: TelegramApiException) {
            }
        }
    }

    fun broadcastToTG(text: String) {
        allowedChats.forEach {
            try {
                val msg = SendMessage(it, text).setParseMode(ParseMode.HTML)
                execute(msg)
            } catch (e: TelegramApiException) {
            }
        }
    }

    private fun mcMessageStr(username: String, text: String): String =
        "<i>$username</i>: $text"

    private fun rawUserMention(user: User): String =
        (if (user.firstName.length < 2) null else user.firstName)
            ?: user.userName
            ?: user.lastName

    private fun telegramUserMention(user: User): String =
        if (user.userName != null) "@${user.userName}"
        else "<a href=\"tg://user?id=${user.id}\">${user.firstName ?: user.lastName}</a>"
}
