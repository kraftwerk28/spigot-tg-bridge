package org.kraftwerk28.spigot_tg_bridge

import com.vdurmont.emoji.EmojiParser
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.meta.TelegramBotsApi
import java.io.File

class Plugin : JavaPlugin() {

    var tgBot: Bot? = null
    var chatToTG: Boolean = false

    init {
        config.run {
            chatToTG = getBoolean("logFromMCtoTG", false)
        }
    }

    override fun onEnable() {
        val configFile = File(
            server.pluginManager.getPlugin(name)!!.dataFolder,
            "config.yml"
        )
        if (!configFile.exists()) {
            logger.warning("No config file found! Saving default one.")
            saveDefaultConfig()
            return
        }

        ApiContextInitializer.init()
        val botsApi = TelegramBotsApi()
        tgBot = Bot(this)

        botsApi.registerBot(tgBot)

        server.pluginManager.registerEvents(EventHandler(this), this)

        // Notify everything about server start
        val startMsg = config.getString("serverStartMessage", null)
        if (startMsg != null) tgBot?.broadcastToTG(startMsg)
        logger.info("Plugin started")
    }

    override fun onDisable() {
        val stopMsg = config.getString("serverStopMessage", null)
        if (stopMsg != null) tgBot?.broadcastToTG(stopMsg)
        logger.info("Plugin stopped")
    }

    fun sendMessageToMC(text: String) {
        val prep = EmojiParser.parseToAliases(text)
        server.broadcastMessage(prep)
    }

    fun sendMessageToMCFrom(username: String, text: String) {
        val prep = EmojiParser.parseToAliases("<$username> $text")
        server.broadcastMessage(prep)
    }
}
