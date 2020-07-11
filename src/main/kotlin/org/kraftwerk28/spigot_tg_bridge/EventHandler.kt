package org.kraftwerk28.spigot_tg_bridge

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class EventHandler(private val plugin: Plugin) : Listener {

    private val joinStr: String
    private val leftStr: String
    private val logJoinLeave: Boolean

    init {
        plugin.config.run {
            joinStr = getString("strings.joined", "joined")!!
            leftStr = getString("strings.left", "left")!!
            logJoinLeave = getBoolean("logJoinLeave", false)
        }
    }

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        if (plugin.chatToTG) {
            plugin.tgBot?.sendMessageToTGFrom(
                event.player.displayName, event.message
            )
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!logJoinLeave) return
        plugin.tgBot?.broadcastToTG(
            "<b>${event.player.displayName}</b> $joinStr."
        )
    }

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        if (!logJoinLeave) return
        plugin.tgBot?.broadcastToTG(
            "<b>${event.player.displayName}</b> b $leftStr."
        )
    }
}