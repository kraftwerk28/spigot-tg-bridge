package org.kraftwerk28.spigot_tg_bridge

import org.bukkit.configuration.file.YamlConfiguration

class Commands(yamlCfg: YamlConfiguration) {
    val time: String
    val online: String
    val chatID: String

    init {
        yamlCfg.run {
            time = getString("commands.time", "time")!!
            online = getString("commands.online", "online")!!
            chatID = getString("commands.chat_id", "chat_id")!!
        }
    }
}