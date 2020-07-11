package org.kraftwerk28.spigot_tg_bridge

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.kraftwerk28.spigot_tg_bridge.Constants as C

class EventHandler(private val plugin: Plugin) : Listener {

    private val joinStr: String
    private val leftStr: String
    private val logJoinLeave: Boolean

    init {
        plugin.config.run {
            joinStr = getString(C.FIELDS.STRINGS.JOINED, C.DEFS.playerJoined)!!
            leftStr = getString(C.FIELDS.STRINGS.LEFT, C.DEFS.playerLeft)!!
            logJoinLeave = getBoolean(C.FIELDS.LOG_JOIN_LEAVE, C.DEFS.logJoinLeave)
        }
    }

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        if (plugin.chatToTG) {
            plugin.tgBot?.sendMessageToTGFrom(
                escapeHTML(event.player.displayName), event.message
            )
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!logJoinLeave) return
        val text = "<b>${escapeHTML(event.player.displayName)}</b> $joinStr."
        plugin.tgBot?.broadcastToTG(text)
    }

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        if (!logJoinLeave) return
        val text = "<b>${escapeHTML(event.player.displayName)}</b> $leftStr."
        plugin.tgBot?.broadcastToTG(text)
    }
}