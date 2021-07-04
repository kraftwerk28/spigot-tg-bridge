package org.kraftwerk28.spigot_tg_bridge

import org.bukkit.configuration.file.FileConfiguration

class BotCommands(cfg: FileConfiguration) {
    val time: String?
    val online: String?
    val chatID: String?

    init {
        cfg.run {
            time = getString("commands.time")
            online = getString("commands.online")
            chatID = getString("commands.chat_id")
        }
    }
}
