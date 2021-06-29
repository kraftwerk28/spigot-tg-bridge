package org.kraftwerk28.spigot_tg_bridge

import com.vdurmont.emoji.EmojiParser
import org.bukkit.plugin.java.JavaPlugin
import java.lang.Exception
import org.kraftwerk28.spigot_tg_bridge.Constants as C

class Plugin : JavaPlugin() {

    var tgBot: TgBot? = null
    val config: Configuration = Configuration()

    override fun onEnable() {
        try {
            config.load(this)
        } catch (e: Exception) {
            logger.warning(e.message)
            return
        }

        if (!config.isEnabled)
            return

        val cmdHandler = CommandHandler(this)
        loadBot()
        tgBot?.let { bot ->
            val eventHandler = EventHandler(bot, config)
            server.pluginManager.registerEvents(eventHandler, this)
        }
        getCommand(C.COMMANDS.PLUGIN_RELOAD)?.setExecutor(cmdHandler)

        // Notify Telegram groups about server start
        config.serverStartMessage?.let { message ->
            tgBot?.sendMessageToTelegram(message)
        }
        logger.info("Plugin started.")
    }

    fun loadBot() {
        tgBot?.let { it.stop() }
        tgBot = TgBot(this, config)
    }

    override fun onDisable() {
        if (!config.isEnabled) return
        config.serverStopMessage?.let {
            tgBot?.sendMessageToTelegram(it)
        }
        logger.info("Plugin stopped.")
    }

    fun sendMessageToMinecraft(text: String, username: String? = null) =
        config.telegramMessageFormat
            .replace(C.MESSAGE_TEXT_PLACEHOLDER, text.escapeEmoji())
            .run {
                username?.let { username ->
                    replace(C.USERNAME_PLACEHOLDER, username.escapeEmoji())
                } ?: this
            }
            .also { server.broadcastMessage(it) }

    fun reload() {
        logger.info(C.INFO.reloading)
        config.reload(this)
        loadBot()
        logger.info(C.INFO.reloadComplete)
    }
}
