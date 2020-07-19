package org.kraftwerk28.spigot_tg_bridge

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import org.kraftwerk28.spigot_tg_bridge.Constants as C

class Configuration(plugin: Plugin) {
    private lateinit var yamlCfg: YamlConfiguration

    var isEnabled: Boolean = false
    var logFromMCtoTG: Boolean = false
    var telegramMessageFormat: String = ""
    var serverStartMessage: String? = null
    var serverStopMessage: String? = null

    // Telegram bot stuff
    var botToken: String = ""
    var botUsername: String = ""
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

    init {
        reload(plugin)
    }

    fun reload(plugin: Plugin) {
        val cfgFile = File(plugin.dataFolder, C.configFilename);
        if (!cfgFile.exists()) {
            cfgFile.parentFile.mkdirs()
            plugin.saveResource(C.configFilename, false);
            throw Exception()
        }

        yamlCfg = YamlConfiguration()
        yamlCfg.load(cfgFile)

        yamlCfg.run {
            isEnabled = getBoolean("enable", true)
            logFromTGtoMC = getBoolean("logFromTGtoMC", true)
            logFromMCtoTG = getBoolean("logFromMCtoTG", true)
            telegramMessageFormat = getString("telegramMessageFormat", "<%username%>: %message%")!!
            allowedChats = getLongList("chats")
            serverStartMessage = getString("serverStartMessage")
            serverStopMessage = getString("serverStopMessage")

            botToken = getString("botToken") ?: throw Exception(C.WARN.noToken)
            botUsername = getString("botUsername") ?: throw Exception(C.WARN.noUsername)

            allowWebhook = getBoolean("useWebhook", false)
            val whCfg = get("webhookConfig")
            if (whCfg is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                webhookConfig = whCfg as Map<String, Any>?
            }

            logJoinLeave = getBoolean("logJoinLeave", false)
            onlineString = getString("strings.online", "Online")!!
            nobodyOnlineString = getString("strings.offline", "Nobody online")!!
            joinString = getString("strings.joined", "joined")
            leaveString = getString("strings.left", "left")
            logDeath = getBoolean("logPlayerDeath", false)
            logPlayerAsleep = getBoolean("logPlayerAsleep", false)

        }

        commands = Commands(yamlCfg)
    }
}