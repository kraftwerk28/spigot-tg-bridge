package org.kraftwerk28.rzcraft_bridge

import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User

class Bot(plugin: Plugin) : TelegramLongPollingBot() {
    private var plugin: Plugin
    private var allowedChats: List<Long> = listOf()
    private var botToken: String = ""
    private var botUsername: String = ""

    init {
        this.plugin = plugin
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

        // /online cmd: shows online players
        if (msg.text.startsWith("/online")) {
            val playerList = plugin.server
                .getOnlinePlayers()
                .mapIndexed { i, s -> "${i + 1}. ${s.displayName}" }
                .joinToString("\n")
            val text =
                if (playerList.length > 0) ("<b>Online:</b>\n" + playerList)
                else "<b>Nobody online...</b>"
            execute(SendMessage(msg.chatId, text).setParseMode("HTML"))
        }

        if (msg.text!!.startsWith("/")) return
        plugin.logger.info("Got text: ${msg.text}")
        plugin.server.broadcastMessage(
            mcMessageStr(rawUserMention(msg.from), msg.text)
        )
    }

    public fun sendMessageToTGFrom(username: String, text: String) {
        allowedChats.forEach {
            execute(SendMessage(it, mcMessageStr(username, text)))
        }
    }

    public fun broadcastToTG(text: String) {
        allowedChats.forEach {
            execute(SendMessage(it, text))
        }
    }

    private fun commandStr(command: String): String =
        "/$command@${botUsername}"

    private fun mcMessageStr(username: String, text: String): String =
        "<$username> $text"

    private fun rawUserMention(user: User): String =
        user.userName ?: user.firstName ?: user.lastName

    private fun telegramUserMention(user: User): String =
        if (user.userName != null) "@${user.userName}"
        else "<a href=\"tg://user?id=${user.id}\">${user.firstName ?: user.lastName}</a>"
}
