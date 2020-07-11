package org.kraftwerk28.spigot_tg_bridge

import org.telegram.telegrambots.meta.api.objects.User

fun escapeHTML(s: String): String =
    s.replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;")

fun telegramUserMention(user: User): String =
    if (user.userName != null) "@${user.userName}"
    else "<a href=\"tg://user?id=${user.id}\">${user.firstName ?: user.lastName}</a>"
