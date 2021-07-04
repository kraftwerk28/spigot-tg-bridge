package org.kraftwerk28.spigot_tg_bridge

import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin
import java.lang.Exception
import org.kraftwerk28.spigot_tg_bridge.Constants as C

class Plugin : JavaPlugin() {
    var tgBot: TgBot? = null
    var eventHandler: EventHandler? = null
    var config: Configuration? = null

    override fun onEnable() {
        try {
            config = Configuration(this)
        } catch (e: Exception) {
            logger.warning(e.message)
            return
        }

        config?.let { config ->
            if (!config.isEnabled) return
            val cmdHandler = CommandHandler(this)
            tgBot?.run { stop() }
            tgBot = TgBot(this, config).also { bot ->
                eventHandler = EventHandler(bot, config).also {
                    server.pluginManager.registerEvents(it, this)
                }
            }
            getCommand(C.COMMANDS.PLUGIN_RELOAD)?.setExecutor(cmdHandler)
            config.serverStartMessage?.let { message ->
                tgBot?.sendMessageToTelegram(message)
            }
        }
    }

    override fun onDisable() {
        config?.let { config ->
            if (!config.isEnabled) return
            config.serverStopMessage?.let {
                tgBot?.sendMessageToTelegram(it, blocking = true)
            }
            eventHandler?.let { HandlerList.unregisterAll(it) }
            tgBot?.run { stop() }
            tgBot = null
        }
    }

    fun sendMessageToMinecraft(
        text: String,
        username: String? = null,
        chatTitle: String? = null,
    ) = config?.run {
        minecraftFormat
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
    }

    fun reload() {
        config?.let { config ->
            if (!config.isEnabled) return
            logger.info(C.INFO.reloading)
            this.config = Configuration(this)
            eventHandler?.let { HandlerList.unregisterAll(it) }
            tgBot?.run { stop() }
            tgBot = TgBot(this, config).also { bot ->
                eventHandler = EventHandler(bot, config).also {
                    server.pluginManager.registerEvents(it, this)
                }
            }
            logger.info(C.INFO.reloadComplete)
        }
    }
}
