package org.kraftwerk28.spigot_tg_bridge

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class EventHandler(
    private val tgBot: TgBot,
    private val config: Configuration
) : Listener {

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        if (!config.logFromMCtoTG) return
        event.run {
            tgBot.sendMessageToTelegram(
                message, player.displayName
            )
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!config.logJoinLeave) return
        val username = event.player.displayName.fullEscape()
        val text = config.joinString.replace("%username%", username)
        tgBot.sendMessageToTelegram(text)
    }

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        if (!config.logJoinLeave) return
        val username = event.player.displayName.fullEscape()
        val text = config.leaveString.replace("%username%", username)
        tgBot.sendMessageToTelegram(text)
    }

    @EventHandler
    fun onPlayerDied(event: PlayerDeathEvent) {
        if (!config.logDeath) return
        event.deathMessage?.let {
            val username = event.entity.displayName.fullEscape()
            val text = it.replace(username, "<i>$username</i>")
            tgBot.sendMessageToTelegram(text)
        }
    }

    @EventHandler
    fun onPlayerAsleep(event: PlayerBedEnterEvent) {
        if (!config.logPlayerAsleep) return
        if (event.bedEnterResult != PlayerBedEnterEvent.BedEnterResult.OK)
            return
        val text = "<i>${event.player.displayName}</i> fell asleep."
        tgBot.sendMessageToTelegram(text)
    }
}
