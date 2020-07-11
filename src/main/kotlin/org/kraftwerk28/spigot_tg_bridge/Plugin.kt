package org.kraftwerk28.spigot_tg_bridge

import com.vdurmont.emoji.EmojiParser
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import org.kraftwerk28.spigot_tg_bridge.Constants as C

class Plugin : JavaPlugin() {

    lateinit var tgBot: TgBot
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

        tgBot = TgBot(this)
        server.pluginManager.registerEvents(EventHandler(this), this)

        // Notify everything about server start
        config.getString(C.FIELDS.SERVER_START_MSG, null)?.let {
            tgBot.broadcastToTG(it)
        }
        logger.info("Plugin started.")
    }

    override fun onDisable() {
        config.getString(C.FIELDS.SERVER_STOP_MSG, null)?.let {
            tgBot.broadcastToTG(it)
        }
        logger.info("Plugin stopped.")
    }

    fun sendMessageToMC(text: String) {
        val prep = EmojiParser.parseToAliases(text)
        server.broadcastMessage(prep)
    }

    fun sendMessageToMCFrom(username: String, text: String) {
        server.broadcastMessage(
            EmojiParser.parseToAliases(
                "<${TgBot.escapeHTML(username)}> $text"
            )
        )
    }
}
