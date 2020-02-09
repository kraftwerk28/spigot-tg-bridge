package org.kraftwerk28.rzcraft_bridge

import org.bukkit.event.Listener
import org.bukkit.event.EventHandler
import org.bukkit.event.player.AsyncPlayerChatEvent


class MessageListener(plugin: Plugin) : Listener {
    private var plugin: Plugin? = null
    init { this.plugin = plugin }

    @EventHandler
    public fun onPlayerChat1(event: AsyncPlayerChatEvent) {
        plugin?.tgBot?.sendMessageToTGFrom(
            event.player.displayName,
            event.message
        )
    }
}
