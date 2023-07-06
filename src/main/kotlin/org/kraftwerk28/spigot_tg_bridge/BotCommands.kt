package org.kraftwerk28.spigot_tg_bridge

import org.bukkit.configuration.file.FileConfiguration

class BotCommands(private val cfg: FileConfiguration) {
    val time: String? get() = cfg.getString("commands.time")
    val online: String? get() = cfg.getString("commands.online")
    val chatID: String? get() = cfg.getString("commands.chat_id")
    val linkIgn: String? get() = cfg.getString("commands.link_ign")
    val getAllLinked: String? get() = cfg.getString("commands.list_linked")
}
