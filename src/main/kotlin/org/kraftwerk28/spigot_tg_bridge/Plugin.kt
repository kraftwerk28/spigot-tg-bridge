package org.kraftwerk28.spigot_tg_bridge

import com.vdurmont.emoji.EmojiParser
import org.bukkit.plugin.java.JavaPlugin
import java.lang.Exception
import org.kraftwerk28.spigot_tg_bridge.Constants as C

class Plugin : JavaPlugin() {

    lateinit var tgBot: TgBot
    lateinit var config: Configuration

    override fun onEnable() {
        try {
            config = Configuration(this)
        } catch (e: Exception) {
            logger.warning(C.WARN.noConfigWarning)
            return
        }

        if (!config.isEnabled) return

        val cmdHandler = CommandHandler(this)
        val eventHandler = EventHandler(this, config)

        tgBot = TgBot(this, config)
        getCommand(C.COMMANDS.PLUGIN_RELOAD)?.setExecutor(cmdHandler)
        server.pluginManager.registerEvents(eventHandler, this)

        // Notify Telegram groups about server start
        config.serverStartMessage?.let {
            tgBot.broadcastToTG(it)
        }
        logger.info("Plugin started.")
    }

    override fun onDisable() {
        if (!config?.isEnabled) return
        config.serverStopMessage?.let {
            tgBot.broadcastToTG(it)
        }
        logger.info("Plugin stopped.")
    }

    fun sendMessageToMC(text: String) {
        val prep = EmojiParser.parseToAliases(text)
        server.broadcastMessage(prep)
    }

    fun sendMessageToMCFrom(username: String, text: String) {
        val prepared = config.telegramMessageFormat
            .replace(C.USERNAME_PLACEHOLDER, emojiEsc(username))
            .replace(C.MESSAGE_TEXT_PLACEHOLDER, emojiEsc(text))
        server.broadcastMessage(prepared)
    }

    fun emojiEsc(text: String) = EmojiParser.parseToAliases(text)

    fun reload() {
        logger.info(C.INFO.reloading)
        config.reload(this)
        tgBot.stop()
        tgBot.start(this, config)
        logger.info(C.INFO.reloadComplete)
    }
}
