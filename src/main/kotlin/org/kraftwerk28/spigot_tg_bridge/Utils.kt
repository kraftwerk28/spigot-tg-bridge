package org.kraftwerk28.spigot_tg_bridge

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.User
import com.vdurmont.emoji.EmojiParser

fun Bot.skipUpdates(lastUpdateID: Long = 0) {
    val newUpdates = getUpdates(lastUpdateID)

    if (newUpdates.isNotEmpty()) {
        val lastUpd = newUpdates.last()
        if (lastUpd !is Update) return
        return skipUpdates(lastUpd.updateId + 1)
    }
}

fun String.escapeHtml() =
    this.replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;")

fun escapeHTML(s: String) = s
    .replace("&", "&amp;")
    .replace(">", "&gt;")
    .replace("<", "&lt;")

fun escapeColorCodes(s: String) = s.replace("\u00A7.".toRegex(), "")

fun fullEscape(s: String) = escapeColorCodes(escapeHTML(s))

fun escapeEmoji(text: String) = EmojiParser.parseToAliases(text)

fun rawUserMention(user: User): String =
    (if (user.firstName.length < 2) null else user.firstName)
        ?: user.username
        ?: user.lastName!!
