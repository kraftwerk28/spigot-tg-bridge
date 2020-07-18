package org.kraftwerk28.spigot_tg_bridge

import com.vdurmont.emoji.EmojiParser
import org.bukkit.ChatColor
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import org.kraftwerk28.spigot_tg_bridge.Constants as C

class Plugin : JavaPlugin() {

    lateinit var tgBot: TgBot
    val chatToTG: Boolean
    var _isEnabled: Boolean = false
    val telegramMessageFormat: String

    init {
        config.run {
            chatToTG = getBoolean(
                C.FIELDS.LOG_FROM_MC_TO_TG,
                C.DEFS.logFromMCtoTG
            )
            _isEnabled = getBoolean(C.FIELDS.ENABLE, C.DEFS.enable)
            telegramMessageFormat = getString(
                C.FIELDS.TELEGRAM_MESSAGE_FORMAT,
                C.DEFS.telegramMessageFormat
            )!!
        }
    }

    override fun onEnable() {
        if (!_isEnabled) return
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
        if (!_isEnabled) return
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
        val prepared = telegramMessageFormat
            .replace(C.USERNAME_PLACEHOLDER, emojiEsc(username))
            .replace(C.MESSAGE_TEXT_PLACEHOLDER, emojiEsc(text))
        server.broadcastMessage(prepared)
    }

    fun emojiEsc(text: String) = EmojiParser.parseToAliases(text)
}
