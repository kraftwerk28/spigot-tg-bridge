package eu.pablob.paper_telegram_bridge

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import java.awt.Color


class EventHandler(
    private val plugin: Plugin,
    private val config: Configuration,
    private val tgBot: TgBot,
) : Listener {

    @EventHandler
    fun onPlayerChat(event: AsyncChatEvent) {
        if (!config.logFromMCtoTG || event.isCancelled) return

        val player = event.player
        val message = PlainTextComponentSerializer.plainText().serialize(event.message())
        // Inventory
        if (message.contains("[") && message.contains("]")) {
            getLogInventory(message, player)
        } else if (message.contains("xaero-waypoint:")) {
            val username = player.name.fullEscape()
            sendXaeroWaypointToTelegram(message, username)
        }
        else {
            sendMessage(message, player.name)
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!config.logJoinLeave) return
        val player = event.player
        val hasPermission = player.hasPermission("tg-bridge.silentjoinleave")
        if (hasPermission) return
        val username = player.playerProfile.name.toString()
        val text = config.joinString.replace("%username%", username)
        sendMessage(text)
    }

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        if (!config.logJoinLeave) return
        val player = event.player
        val hasPermission = player.hasPermission("tg-bridge.silentjoinleave")
        if (hasPermission) return
        val username = player.playerProfile.name.toString().fullEscape()
        val text = config.leaveString.replace("%username%", username)
        sendMessage(text)
    }

    @EventHandler
    fun onPlayerDied(event: PlayerDeathEvent) {
        if (!config.logDeath) return
        val hasPermission = event.entity.hasPermission("tg-bridge.silentjoinleave")
        if (hasPermission) return
        val deathMessage = event.deathMessage()?.let { PlainTextComponentSerializer.plainText().serialize(it) }
        deathMessage.let {
            val username = event.entity.playerProfile.name.toString().fullEscape()
            val text = it!!.replace(username, "<i>$username</i>")
            sendMessage(text)
        }
    }

    @EventHandler
    fun onPlayerAsleep(event: PlayerBedEnterEvent) {
        if (!config.logPlayerAsleep) return
        val player = event.player
        val hasPermission = player.hasPermission("tg-bridge.silentjoinleave")
        if (hasPermission) return
        if (event.bedEnterResult != PlayerBedEnterEvent.BedEnterResult.OK) return
        val text = "<i>${player.playerProfile.name.toString()}</i> fell asleep."
        sendMessage(text)
    }

    @EventHandler
    fun onPlayerLogin(event: PlayerLoginEvent) {
        if (!config.logWhitelistKick) return
        // Check if login was disallowed due to whitelist
        if (event.result == PlayerLoginEvent.Result.KICK_WHITELIST) {
            val username = event.player.name.fullEscape()
            val kickMessage = PlainTextComponentSerializer.plainText().serialize(event.kickMessage())
            val text = "⚠️ <i>$username</i> tried to join but was kicked: $kickMessage"
            plugin.logger.info("Sending whitelist kick message to Telegram for $username")
            sendMessage(text)
        }
    }

    private fun sendMessage(text: String, username: String? = null) = plugin.launch {
        tgBot.sendMessageToTelegram(text, username)
    }


    @EventHandler
    fun onPlayerAdvancementDone(event: PlayerAdvancementDoneEvent) {
        if (!config.logPlayerAdvancement) return

        val display = event.advancement.display ?: return // Ensure advancement has a display
        val item = display.icon()
        if (!display.doesAnnounceToChat()) return

        val advancementName =
            PlainTextComponentSerializer.plainText().serialize(event.advancement.displayName()).replace("[", "")
                .replace("]", "")
        val username = event.player.playerProfile.name.toString().fullEscape()
        val frameType = display.frame().name.lowercase() // Get advancement frame type ("task", "goal", "challenge")
        val description = PlainTextComponentSerializer.plainText().serialize(display.description())
        val message = when (frameType) {
            "goal" -> config.goalString
            "challenge" -> config.challengeString
            else -> config.advancementString // Default to "task"
        }.replace("%username%", username).replace("%advancement%", advancementName)
            .replace("%description%", description)
        val textColor = Color.decode(display.frame().color().asHexString())
        sendAdvancement(item, advancementName, frameType, textColor, message)
    }

    private fun sendAdvancement(item: ItemStack?, title: String, frameType: String, textColor: Color, message: String) {
        plugin.launch {
            val imageBytes = AdvancementRenderer().renderAdvancement(title, frameType, item, textColor)

            if (imageBytes.isNotEmpty()) {
                tgBot.sendPhotoToTelegram(imageBytes, message)
            } else {
                tgBot.sendMessageToTelegram(message) // Fallback if rendering fails
            }
        }
    }


    private fun getLogInventory(message: String, player: Player) {
        if (!config.logInventory) return

        val lowerMessage = message.lowercase()

        val playerName = player.playerProfile.name

        fun formatCaption(label: String, msgBefore: String, msgAfter: String, showPlayerName: Boolean = true): String {
            val playerNameLabel = if(showPlayerName) "$playerName’s " else ""
            return "$playerName: $msgBefore[$playerNameLabel$label]$msgAfter"
        }


        when {
            "[inv]" in lowerMessage -> plugin.launch {
                val (userMessageBefore, userMessageAfter) = userMessageBeforeAfter(message, "[inv]")
                val image = InventoryRenderer().renderInventoryToFile(player.inventory)
                tgBot.sendPhotoToTelegram(image, formatCaption("Inventory", userMessageBefore, userMessageAfter))
            }

            "[ender]" in lowerMessage -> plugin.launch {
                val (userMessageBefore, userMessageAfter) = userMessageBeforeAfter(message, "[inv]")
                val image = EnderChestRenderer().renderEnderChestToFile(player.enderChest)
                tgBot.sendPhotoToTelegram(image, formatCaption("Ender Chest", userMessageBefore, userMessageAfter))
            }

            "[item]" in lowerMessage -> plugin.launch {
                val item = player.inventory.itemInMainHand
                if (item.type.name.lowercase() == "written_book" || item.type.name.lowercase() == "writable_book") {
                    val (bookDirectory, caption) = BookRenderer(plugin).renderBookToFile(item)
                    if (bookDirectory != null) {
                        tgBot.sendImageWithKeyboard(1, bookDirectory, caption)
                    }
                    return@launch
                } else if (item.type.name.lowercase() == "shulker_box") {
                    val shulkerBoxContents = getShulkerBoxContents(item)
                    val (userMessageBefore, userMessageAfter) = userMessageBeforeAfter(message, "[item]")
                    val image = EnderChestRenderer().renderEnderChestToFile(shulkerBoxContents)
                    tgBot.sendPhotoToTelegram(image, formatCaption("Shulker Box", userMessageBefore, userMessageAfter, false))
                    return@launch
                }

                val (image, itemName) = ItemRenderer().renderItemToFile(item)
                val formattedName = itemName.substringBefore('(').trim()
                val amountSuffix = if (item.amount > 1) " x ${item.amount}" else ""
                val (userMessageBefore, userMessageAfter) = userMessageBeforeAfter(message, "[item]")
                tgBot.sendPhotoToTelegram(
                    image, "$playerName: $userMessageBefore[$formattedName$amountSuffix]$userMessageAfter"
                )
            }

            else -> {
                // TODO: temporary fix to avoid weird messages on telegram when executing some interactive chat commands.
                // Should be handled via config.
                val otherCommands = arrayOf("[ping]", "[pos]", "[gametime]", "[time]", "[vault]")
                if (!otherCommands.any { lowerMessage.contains(it) }) {
                    // Default case if no tags are found
                    sendMessage(message, playerName)
                }

            }
        }
    }

    fun getShulkerBoxContents(item: ItemStack): Inventory {
        // Get the item meta and cast to BlockStateMeta
        val meta = item.itemMeta as? BlockStateMeta

        // Get the stored block state and cast to ShulkerBox
        val shulkerBox = meta!!.blockState as? ShulkerBox

        // Return non-null contents
        return shulkerBox!!.inventory
    }

    private fun sendXaeroWaypointToTelegram(xaeroWaypointMessage: String = "xaero-waypoint:GUAYPOINT:G:12688:128:12094:0:false:0:Internal-the-nether", userName: String = "Unknown") {
        // Example: xaero-waypoint:GUAYPOINT:G:12688:128:12094:0:false:0:Internal-the-nether
        val waypointData = xaeroWaypointMessage.substringAfter("xaero-waypoint:").split(":")
        val waypointName = waypointData[0]
        val world = waypointData[8].replace("Internal-", "")
        val x = waypointData[3]
        val y = waypointData[4]
        val z = waypointData[5]
        val waypointMessage = config.waypointMessageString
            .replace("%username%", userName)
            .replace("%waypointName%", waypointName)
            .replace("%world%", world)
            .replace("%x%", x)
            .replace("%y%", y)
            .replace("%z%", z)
            .replace("%raw%", xaeroWaypointMessage) // For debugging purposes.
            .trimIndent()
        sendMessage(waypointMessage)
    }

    private fun userMessageBeforeAfter(message: String, tag: String): Pair<String, String> {
        var userMessageBefore = message.substringBefore(tag)
        var userMessageAfter = message.substringAfter(tag)

        // Handle InteractiveChat plugin formatting.
        if ("<" in message && ">" in message) {
            val newTag = message.substringAfter("<").substringBefore(">")
            userMessageBefore = message.substringBefore("<$newTag>")
            userMessageAfter = message.substringAfter("<$newTag>")
        }

        return Pair(userMessageBefore, userMessageAfter)
    }


}
