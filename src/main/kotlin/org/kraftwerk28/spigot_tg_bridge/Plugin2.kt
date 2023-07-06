package org.kraftwerk28.spigot_tg_bridge

import org.bukkit.plugin.java.JavaPlugin

class Plugin2 : JavaPlugin() {
    override fun onEnable() {
        super.onEnable()
        logger.info("Enabling plugin")
    }

    override fun onDisable() {
        super.onDisable()
        logger.info("Disabling plugin")
    }
}