package org.kraftwerk28.spigot_tg_bridge

import com.vdurmont.emoji.EmojiParser

fun String.escapeHtml() = this
    .replace("&", "&amp;")
    .replace(">", "&gt;")
    .replace("<", "&lt;")

fun String.escapeHTML() = this
    .replace("&", "&amp;")
    .replace(">", "&gt;")
    .replace("<", "&lt;")

fun String.escapeColorCodes() = replace("\u00A7.".toRegex(), "")

fun String.fullEscape() = escapeHTML().escapeColorCodes()

fun String.escapeEmoji() = EmojiParser.parseToAliases(this)

fun TgApiService.User.rawUserMention(): String =
    (if (firstName.length < 2) null else firstName)
        ?: username
        ?: lastName!!
