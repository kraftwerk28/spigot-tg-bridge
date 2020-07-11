package org.kraftwerk28.spigot_tg_bridge

import com.vdurmont.emoji.EmojiParser
import org.bukkit.plugin.java.JavaPlugin
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.meta.TelegramBotsApi
import java.io.File
import org.kraftwerk28.spigot_tg_bridge.Constants as C

class Plugin : JavaPlugin() {

    var tgBot: Bot? = null
    var chatToTG: Boolean = false

    init {
        config.run {
            chatToTG = getBoolean(
                C.FIELDS.LOG_FROM_MC_TO_TG,
                C.DEFS.logFromMCtoTG
            )
        }
    }

    override fun onEnable() {
        val configFile = File(
            server.pluginManager.getPlugin(name)!!.dataFolder,
            C.configFilename
        )
        if (!configFile.exists()) {
            logger.warning(C.WARN.noConfigWarning)
            saveDefaultConfig()
            return
        }

        ApiContextInitializer.init()
        val botsApi = TelegramBotsApi()
        tgBot = Bot(this)

        botsApi.registerBot(tgBot)

        server.pluginManager.registerEvents(EventHandler(this), this)

        // Notify everything about server start
        config.getString(C.FIELDS.SERVER_START_MSG, null)?.let {
            logger.info("Server start message: $it")
            tgBot?.broadcastToTG(it)
        }
        logger.info("Plugin started.")
    }

    override fun onDisable() {
        config.getString(C.FIELDS.SERVER_STOP_MSG, null)?.let {
            logger.info("Server stop message: $it")
            tgBot?.broadcastToTG(it)
        }
        logger.info("Plugin stopped.")
    }

    fun sendMessageToMC(text: String) {
        val prep = EmojiParser.parseToAliases(text)
        server.broadcastMessage(prep)
    }

    fun sendMessageToMCFrom(username: String, text: String) {
        val text = run {
            val text = "<${escapeHTML(username)}> $text"
            EmojiParser.parseToAliases(text)
        }
        server.broadcastMessage(text)
    }
}
