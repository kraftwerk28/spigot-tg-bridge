package org.kraftwerk28.spigot_tg_bridge

import org.bukkit.event.HandlerList
import java.lang.Exception
import org.kraftwerk28.spigot_tg_bridge.Constants as C

class Plugin : AsyncJavaPlugin() {
    var tgBot: TgBot? = null
    private var eventHandler: EventHandler? = null
    var config: Configuration? = null
    var ignAuth: IgnAuth? = null

    override suspend fun onEnableAsync() = try {
        config = Configuration(this).also { config ->
            if (!config.isEnabled) return

            if (config.enableIgnAuth) {
                val dbFilePath = dataFolder.resolve("spigot-tg-bridge.sqlite")
                ignAuth = IgnAuth(
                    fileName = dbFilePath.absolutePath,
                    plugin = this,
                )
            }

            tgBot?.run { stop() }
            tgBot = TgBot(this, config).also { bot ->
                bot.startPolling()
                eventHandler = EventHandler(this, config, bot).also {
                    server.pluginManager.registerEvents(it, this)
                }
            }

            getCommand(C.COMMANDS.PLUGIN_RELOAD)?.run {
                setExecutor(CommandHandler(this@Plugin))
            }
            config.serverStartMessage?.let {
                tgBot?.sendMessageToTelegram(it)
            }
        }
    } catch (e: Exception) {
        // Configuration file is missing or incomplete
        logger.warning(e.message)
    }

    override suspend fun onDisableAsync() {
        config?.let fn@{ config ->
            if (!config.isEnabled)
                return@fn
            config.serverStopMessage?.let {
                tgBot?.sendMessageToTelegram(it)
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

    suspend fun reload() {
        config = Configuration(this).also { config ->
            if (!config.isEnabled) return
            logger.info(C.INFO.reloading)
            eventHandler?.let { HandlerList.unregisterAll(it) }
            tgBot?.run { stop() }
            tgBot = TgBot(this, config).also { bot ->
                eventHandler = EventHandler(this, config, bot).also {
                    server.pluginManager.registerEvents(it, this)
                }
            }
            logger.info(C.INFO.reloadComplete)
        }
    }
}
