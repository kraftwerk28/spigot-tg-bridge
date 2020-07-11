package org.kraftwerk28.spigot_tg_bridge

import org.kraftwerk28.spigot_tg_bridge.Constants as C

class Commands(plugin: Plugin) {
    val time: String
    val online: String
    init {
        plugin.config.run {
            time = getString(
                C.FIELDS.COMMANDS.TIME,
                C.DEFS.COMMANDS.TIME
            )!!
            online = getString(
                C.FIELDS.COMMANDS.ONLINE,
                C.DEFS.COMMANDS.ONLINE
            )!!
        }
    }
}