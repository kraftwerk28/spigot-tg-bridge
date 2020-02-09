package org.kraftwerk28.rzcraft_bridge

import com.vdurmont.emoji.EmojiParser
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.meta.TelegramBotsApi
import java.io.File

class Plugin : JavaPlugin(), Listener {

    private var tgBot: Bot? = null
    private var chatToTG: Boolean = false

    override fun onEnable() {
        if (!File("plugins/${name}/config.yml").exists()) {
            logger.warning("No config file found! Saving default one.")
            saveDefaultConfig()
            return
        }

        ApiContextInitializer.init()
        val botsApi = TelegramBotsApi()
        tgBot = Bot(this)
        chatToTG = config.getBoolean("logFromMCtoTG", false)

        botsApi.registerBot(tgBot)

        server.pluginManager.registerEvents(this, this)

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

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        if (chatToTG)
            tgBot?.sendMessageToTGFrom(event.player.displayName, event.message)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (config.getBoolean("logJoinLeave", false)) {
            val joinStr = config.getString("strings.joined", "joined")
            tgBot?.broadcastToTG("${event.player.displayName} $joinStr.")
        }
    }

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        if (config.getBoolean("logJoinLeave", false)) {
            val leftStr = config.getString("strings.left", "joined")
            tgBot?.broadcastToTG("${event.player.displayName} $leftStr.")
        }
    }
}
