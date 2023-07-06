package org.kraftwerk28.spigot_tg_bridge

import kotlinx.coroutines.launch
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*

class EventHandler(
    private val plugin: Plugin,
    private val config: Configuration,
    private val tgBot: TgBot,
) : Listener {

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        if (!config.logFromMCtoTG || event.isCancelled) return
        event.run {
            sendMessage(message, player.displayName)
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!config.logJoinLeave) return
        val username = event.player.displayName.fullEscape()
        val text = config.joinString.replace("%username%", username)
        sendMessage(text)
    }

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        if (!config.logJoinLeave) return
        val username = event.player.displayName.fullEscape()
        val text = config.leaveString.replace("%username%", username)
        sendMessage(text)
    }

    @EventHandler
    fun onPlayerDied(event: PlayerDeathEvent) {
        if (!config.logDeath) return
        event.deathMessage?.let {
            val username = event.entity.displayName.fullEscape()
            val text = it.replace(username, "<i>$username</i>")
            sendMessage(text)
        }
    }

    @EventHandler
    fun onPlayerAsleep(event: PlayerBedEnterEvent) {
        if (!config.logPlayerAsleep) return
        if (event.bedEnterResult != PlayerBedEnterEvent.BedEnterResult.OK)
            return
        val text = "<i>${event.player.displayName}</i> fell asleep."
        sendMessage(text)
    }

    private fun sendMessage(text: String, username: String? = null) = plugin.launch {
        tgBot.sendMessageToTelegram(text, username)
    }
}
