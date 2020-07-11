package org.kraftwerk28.spigot_tg_bridge

import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

class Bot(private var plugin: Plugin) : TelegramLongPollingBot() {

    private val allowedChats: List<Long>
    private val chatToMC: Boolean
    private val botToken: String
    private val botUsername: String
    init {
        plugin.config.run {
            allowedChats = getLongList("chats")
            chatToMC = getBoolean("logFromTGtoMC", false)
            botToken = getString("botToken") ?: throw Exception("Bot token must be defined.")
            botUsername = getString("botUsername") ?: throw Exception("Bot username must be defined.")
        }
    }

    override fun getBotToken() = botToken

    override fun getBotUsername() = botUsername

    override fun onUpdateReceived(update: Update?) {
        val msg = update?.message
        plugin.logger.info("chat id: ${msg?.chatId}, message id: ${msg?.messageId}")
        if (msg == null || msg.text == null) return
        if (!allowedChats.contains(msg.chatId)) return

        // cmd shows online players
        if (msg.text.startsWith("/online")) {
            val playerList = plugin.server.onlinePlayers
            val playerStr = plugin.server
                .onlinePlayers
                .mapIndexed { i, s -> "${i + 1}. ${s.displayName}" }
                .joinToString("\n")
            val onlineStr = plugin.config.getString(
                "strings.online",
                "Online"
            )!!
            val offlineStr = plugin.config.getString(
                "strings.nobodyOnline",
                "Nobody online"
            )!!
            val text =
                if (playerList.isNotEmpty()) "$onlineStr:\n$playerStr"
                else offlineStr
            reply(msg, text) { it.replyToMessageId = msg.messageId }
        }
        if (msg.text.startsWith("/time")) {
            val t = plugin.server.worlds[0].time
            var text = when {
                t <= 12000 -> "\uD83C\uDFDE Day"
                t <= 13800 -> "\uD83C\uDF06 Sunset"
                t <= 22200 -> "\uD83C\uDF03 Night"
                t <= 24000 -> "\uD83C\uDF05 Sunrise"
                else -> ""
            }
            text += " ($t)"
            reply(msg, text) { it.replyToMessageId = msg.messageId }
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

    private fun reply(
        msg: Message,
        text: String,
        prep: ((sender: SendMessage) -> Unit)? = null
    ): Message {
        val snd = SendMessage(msg.chatId, text).setParseMode(ParseMode.HTML)
        if (prep != null) prep(snd)
        return execute(snd)
    }

    private fun checkAdmin(msg: Message): Boolean {
        val admins = execute(GetChatAdministrators().setChatId(msg.chatId))
        return admins.any { it.user.id == msg.from.id }
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
