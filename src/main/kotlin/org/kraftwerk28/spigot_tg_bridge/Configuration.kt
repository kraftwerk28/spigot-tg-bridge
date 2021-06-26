package org.kraftwerk28.spigot_tg_bridge

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import org.kraftwerk28.spigot_tg_bridge.Constants as C

class Configuration {
    private lateinit var yamlCfg: YamlConfiguration

    var isEnabled: Boolean = false
    var logFromMCtoTG: Boolean = false
    var telegramMessageFormat: String = ""
    var minecraftMessageFormat: String = ""
    var serverStartMessage: String? = null
    var serverStopMessage: String? = null

    // Telegram bot stuff
    var botToken: String = ""
    var allowedChats: List<Long> = listOf()
    var logFromTGtoMC: Boolean = false
    var allowWebhook: Boolean = false
    var webhookConfig: Map<String, Any>? = null

    var logJoinLeave: Boolean = false
    var joinString: String? = null
    var leaveString: String? = null
    var logDeath: Boolean = false
    var logPlayerAsleep: Boolean = false
    var onlineString: String = ""
    var nobodyOnlineString: String = ""

    lateinit var commands: Commands

    fun reload(plugin: Plugin) {
        val cfgFile = File(plugin.dataFolder, C.configFilename);
        if (!cfgFile.exists()) {
            cfgFile.parentFile.mkdirs()
            plugin.saveResource(C.configFilename, false);
            throw Exception(C.WARN.noConfigWarning)
        }

        yamlCfg = YamlConfiguration()
        yamlCfg.load(cfgFile)

        yamlCfg.run {
            isEnabled = getBoolean("enable", true)
            logFromTGtoMC = getBoolean("logFromTGtoMC", true)
            logFromMCtoTG = getBoolean("logFromMCtoTG", true)
            telegramMessageFormat = getString("telegramMessageFormat", "<%username%>: %message%")!!
            minecraftMessageFormat = getString("minecraftMessageFormat", "<i>%username%</i>: %message%")!!
            allowedChats = getLongList("chats")
            serverStartMessage = getString("serverStartMessage")
            serverStopMessage = getString("serverStopMessage")
            botToken = getString("botToken") ?: throw Exception(C.WARN.noToken)
            allowWebhook = getBoolean("useWebhook", false)
            val whCfg = get("webhookConfig")
            if (whCfg is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                webhookConfig = whCfg as Map<String, Any>?
            }

            logJoinLeave = getBoolean("logJoinLeave", false)
            onlineString = getString("strings.online", "Online")!!
            nobodyOnlineString = getString("strings.nobodyOnline", "Nobody online")!!
            joinString = getString("strings.joined", "<i>%username%</i> joined.")
            leaveString = getString("strings.left", "<i>%username%</i> left.")
            logDeath = getBoolean("logPlayerDeath", false)
            logPlayerAsleep = getBoolean("logPlayerAsleep", false)

        }

        commands = Commands(yamlCfg)
    }

    fun load(plugin: Plugin) = reload(plugin)
}
