package org.kraftwerk28.spigot_tg_bridge

import org.telegram.telegrambots.meta.api.objects.User

fun escapeHTML(s: String): String =
    s.replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;")

