package org.kraftwerk28.spigot_tg_bridge

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.kraftwerk28.spigot_tg_bridge.Constants as C

class EventHandler(private val plugin: Plugin) : Listener {

    private val joinStr: String
    private val leftStr: String
    private val logJoinLeave: Boolean
    private val logDeathMessage: Boolean
    private val logPlayerAsleep: Boolean

    init {
        plugin.config.run {
            joinStr = getString(C.FIELDS.STRINGS.JOINED, C.DEFS.playerJoined)!!
            leftStr = getString(C.FIELDS.STRINGS.LEFT, C.DEFS.playerLeft)!!
            logJoinLeave = getBoolean(C.FIELDS.LOG_JOIN_LEAVE, C.DEFS.logJoinLeave)
            logDeathMessage = getBoolean(C.FIELDS.LOG_PLAYER_DEATH, C.DEFS.logPlayerDeath)
            logPlayerAsleep = getBoolean(C.FIELDS.LOG_PLAYER_ASLEEP, C.DEFS.logPlayerAsleep)
        }
        plugin.logger.info("Log death message: $logDeathMessage")
    }

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        if (plugin.chatToTG) {
            plugin.tgBot.sendMessageToTGFrom(
                event.player.displayName, event.message
            )
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!logJoinLeave) return
        val text = "<b>${TgBot.escapeHTML(event.player.displayName)}</b> $joinStr."
        plugin.tgBot.broadcastToTG(text)
    }

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        if (!logJoinLeave) return
        val text = "<b>${TgBot.escapeHTML(event.player.displayName)}</b> $leftStr."
        plugin.tgBot.broadcastToTG(text)
    }

    @EventHandler
    fun onPlayerDied(event: PlayerDeathEvent) {
        if (!logDeathMessage) return
        event.deathMessage?.let {
            val plName = event.entity.displayName
            val text = it.replace(plName, "<b>$plName</b>")
            plugin.tgBot.broadcastToTG(text)
        }
    }

    @EventHandler
    fun onPlayerAsleep(event: PlayerBedEnterEvent) {
        if (!logPlayerAsleep) return
        if (event.bedEnterResult != PlayerBedEnterEvent.BedEnterResult.OK) return
        val text = "<b>${event.player.displayName}</b> fell asleep."
        plugin.tgBot.broadcastToTG(text)
    }
}