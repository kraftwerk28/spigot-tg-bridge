package org.kraftwerk28.rzcraft_bridge

import org.bukkit.plugin.java.JavaPlugin
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.meta.TelegramBotsApi
import java.io.File
import java.nio.file.Paths

class Plugin : JavaPlugin() {
    var tgBot: Bot? = null
    override fun onEnable() {
        super.onEnable()
        if (!File("plugins/${name}/config.yml").exists()) {
            logger.warning("No config file found! Saving default one.")
            saveDefaultConfig()
            return
        }
        logger.info(config.getLongList("chats").toString())
        ApiContextInitializer.init()
        val botsApi = TelegramBotsApi()
        tgBot = Bot(this)
        botsApi.registerBot(tgBot)

        server.pluginManager.registerEvents(MessageListener(this), this)
        val startMsg = config.getString("serverStartMessage")
        if (startMsg != null)
            tgBot?.broadcastToTG(startMsg)
        logger.info("Plugin started")
    }

    override fun onDisable() {
        super.onDisable()
        val stopMsg = config.getString("serverStopMessage")
        if (stopMsg != null)
            tgBot?.broadcastToTG(stopMsg)
        logger.info("Plugin stopped")
    }
}
