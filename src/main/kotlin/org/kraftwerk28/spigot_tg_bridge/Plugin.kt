package org.kraftwerk28.spigot_tg_bridge

import com.vdurmont.emoji.EmojiParser
import org.bukkit.plugin.java.JavaPlugin
import java.lang.Exception
import org.kraftwerk28.spigot_tg_bridge.Constants as C

class Plugin : JavaPlugin() {

    var tgBot: TgBot? = null
    lateinit var config: Configuration

    override fun onEnable() {
        try {
            config = Configuration(this)
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

        config.serverStartMessage?.let { message ->
            tgBot?.sendMessageToTelegram(message)
        }
    }

    fun loadBot() {
        tgBot?.run { stop() }
        tgBot = TgBot(this, config)
    }

    override fun onDisable() {
        if (!config.isEnabled) return
        config.serverStopMessage?.let {
            tgBot?.sendMessageToTelegram(it, blocking = true)
        }
        tgBot?.run { stop() }
    }

    fun sendMessageToMinecraft(
        text: String,
        username: String? = null,
        chatTitle: String? = null,
    ) =
        config.minecraftFormat
            .replace(C.MESSAGE_TEXT_PLACEHOLDER, text.escapeEmoji())
            .run {
                username?.let {
                    replace(C.USERNAME_PLACEHOLDER, it.escapeEmoji())
                } ?: this
            }
            .run {
                chatTitle?.let {
                    replace(C.CHAT_TITLE_PLACEHOLDER, it)
                } ?: this
            }
            .also { server.broadcastMessage(it) }

    fun reload() {
        logger.info(C.INFO.reloading)
        config = Configuration(this)
        loadBot()
        logger.info(C.INFO.reloadComplete)
    }
}
