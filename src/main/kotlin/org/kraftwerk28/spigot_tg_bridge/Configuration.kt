package org.kraftwerk28.spigot_tg_bridge

import java.io.File
import org.kraftwerk28.spigot_tg_bridge.Constants as C

class Configuration(plugin: Plugin) {
    val isEnabled: Boolean
    val logFromMCtoTG: Boolean
    val telegramFormat: String
    val minecraftFormat: String
    val serverStartMessage: String?
    val serverStopMessage: String?
    val logJoinLeave: Boolean
    val joinString: String
    val leaveString: String
    val logDeath: Boolean
    val logPlayerAsleep: Boolean
    val onlineString: String
    val nobodyOnlineString: String

    // Telegram bot stuff
    val botToken: String
    val allowedChats: List<Long>
    val logFromTGtoMC: Boolean
    val allowWebhook: Boolean
    val webhookConfig: Map<String, Any>?

    var commands: BotCommands

    init {
        val cfgFile = File(plugin.dataFolder, C.configFilename)
        if (!cfgFile.exists()) {
            cfgFile.parentFile.mkdirs()
            plugin.saveDefaultConfig()
            // plugin.saveResource(C.configFilename, false);
            throw Exception(C.WARN.noConfigWarning)
        }
        val pluginConfig = plugin.getConfig()
        pluginConfig.load(cfgFile)

        pluginConfig.getString("minecraftMessageFormat")?.let {
            plugin.logger.warning(
                """
                Config option "minecraftMessageFormat" is deprecated.
                Moved it to new key "telegramFormat"
                """.trimIndent().replace('\n', ' ')
            )
            pluginConfig.set("telegramFormat", it)
            pluginConfig.set("minecraftMessageFormat", null)
            plugin.saveConfig()
        }

        pluginConfig.getString("telegramMessageFormat")?.let {
            plugin.logger.warning(
                """
                Config option "telegramMessageFormat" is deprecated.
                Moved it to new key "minecraftFormat"
                """.trimIndent().replace('\n', ' ')
            )
            pluginConfig.set("minecraftFormat", it)
            pluginConfig.set("telegramMessageFormat", null)
            plugin.saveConfig()
        }

        pluginConfig.run {
            isEnabled = getBoolean("enable", true)
            serverStartMessage = getString("serverStartMessage")
            serverStopMessage = getString("serverStopMessage")
            logFromTGtoMC = getBoolean("logFromTGtoMC", true)
            logFromMCtoTG = getBoolean("logFromMCtoTG", true)
            telegramFormat = getString(
                "telegramFormat",
                "<i>%username%</i>: %message%",
            )!!
            minecraftFormat = getString(
                "minecraftFormat",
                "<%username%>: %message%",
            )!!
            // isEnabled = getBoolean("enable", true)
            allowedChats = getLongList("chats")
            botToken = getString("botToken") ?: throw Exception(C.WARN.noToken)
            allowWebhook = getBoolean("useWebhook", false)
            @Suppress("unchecked_cast")
            webhookConfig = get("webhookConfig") as Map<String, Any>?
            logJoinLeave = getBoolean("logJoinLeave", false)
            onlineString = getString("strings.online", "Online")!!
            nobodyOnlineString = getString(
                "strings.nobodyOnline",
                "Nobody online",
            )!!
            joinString = getString(
                "strings.joined",
                "<i>%username%</i> joined.",
            )!!
            leaveString = getString("strings.left", "<i>%username%</i> left.")!!
            logDeath = getBoolean("logPlayerDeath", false)
            logPlayerAsleep = getBoolean("logPlayerAsleep", false)
            commands = BotCommands(this)
        }
    }

    companion object {
    }
}
