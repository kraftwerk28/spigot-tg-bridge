package org.kraftwerk28.spigot_tg_bridge

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class EventHandler(
    private val plugin: Plugin,
    private val config: Configuration
) : Listener {

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        if (config.logFromMCtoTG) {
            plugin.tgBot.sendMessageToTGFrom(
                event.player.displayName, event.message
            )
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!config.logJoinLeave) return
        val text = "<i>${TgBot.escape(event.player.displayName)}</i> " +
                "${config.joinString}."
        plugin.tgBot.broadcastToTG(text)
    }

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        if (!config.logJoinLeave) return
        val text = "<i>${TgBot.escape(event.player.displayName)}</i> " +
                "${config.leaveString}."
        plugin.tgBot.broadcastToTG(text)
    }

    @EventHandler
    fun onPlayerDied(event: PlayerDeathEvent) {
        if (!config.logDeath) return
        event.deathMessage?.let {
            val plName = event.entity.displayName
            val text = it.replace(plName, "<i>$plName</i>")
            plugin.tgBot.broadcastToTG(text)
        }
    }

    @EventHandler
    fun onPlayerAsleep(event: PlayerBedEnterEvent) {
        if (!config.logPlayerAsleep) return
        if (event.bedEnterResult != PlayerBedEnterEvent.BedEnterResult.OK) return
        val text = "<i>${event.player.displayName}</i> fell asleep."
        plugin.tgBot.broadcastToTG(text)
    }
}