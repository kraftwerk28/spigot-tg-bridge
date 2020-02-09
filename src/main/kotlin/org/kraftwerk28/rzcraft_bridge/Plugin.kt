package org.kraftwerk28.rzcraft_bridge

import org.bukkit.plugin.java.JavaPlugin
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.meta.TelegramBotsApi
import java.io.File

class Plugin : JavaPlugin() {
    var tgBot: Bot? = null
    override fun onEnable() {
        if (!File("plugins/${name}/config.yml").exists()) {
            logger.warning("No config file found! Saving default one.")
            saveDefaultConfig()
            return
        }

        ApiContextInitializer.init()
        val botsApi = TelegramBotsApi()
        tgBot = Bot(this)
        botsApi.registerBot(tgBot)

        server.pluginManager.registerEvents(MessageListener(this), this)

        val startMsg = config.getString("serverStartMessage", null)
        if (startMsg != null) tgBot?.broadcastToTG(startMsg)
        logger.info("Plugin started")
    }

    override fun onDisable() {
        val stopMsg = config.getString("serverStopMessage", null)
        if (stopMsg != null) tgBot?.broadcastToTG(stopMsg)
        logger.info("Plugin stopped")
    }
}
