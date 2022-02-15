package org.kraftwerk28.spigot_tg_bridge

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runInterruptible
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds
import org.kraftwerk28.spigot_tg_bridge.Constants as C

class Configuration(plugin: Plugin) : YamlConfiguration() {
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
    val enableIgnAuth: Boolean
    val silentMessages: Boolean?

    // Telegram bot stuff
    val botToken: String
    val allowedChats: List<Long>
    val logFromTGtoMC: Boolean
    val allowWebhook: Boolean
    val webhookConfig: Map<String, Any>?
    val pollTimeout: Int
    val apiOrigin: String
    val debugHttp: Boolean

    var commands: BotCommands

    init {
        val cfgFile = File(plugin.dataFolder, C.configFilename)
        if (!cfgFile.exists()) {
            cfgFile.parentFile.mkdirs()
            plugin.saveDefaultConfig()
            // plugin.saveResource(C.configFilename, false);
            throw Exception(C.WARN.noConfigWarning)
        }

        load(cfgFile)

        if (!getBoolean("disableConfigWatch", false)) {
            try {
                val watchService = FileSystems.getDefault().newWatchService()
                val cfgPath = cfgFile.parentFile.toPath()
                val pathKey = cfgPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)
                plugin.launch {
                    loop@ while (true) {
                        try {
                            val watchKey = runInterruptible { watchService.take() }
                            val events = watchKey.pollEvents()
                            events.find {
                                it.kind() == StandardWatchEventKinds.ENTRY_MODIFY
                            }?.let {
                                plugin.reload()
                            }
                        } catch (e: Exception) {
                            when (e) {
                                is CancellationException -> break@loop
                                else -> {
                                    e.printStackTrace()
                                    continue@loop
                                }
                            }
                        }
                    }
                    pathKey.cancel()
                }
            } catch (e: Exception) {
                plugin.logger.info("Failed to set up watch on config file")
            }
        }

        getString("minecraftMessageFormat")?.let {
            plugin.logger.warning(
                """
                Config option "minecraftMessageFormat" is deprecated.
                Moved it to new key "telegramFormat"
                """.trimIndent().replace('\n', ' ')
            )
            set("telegramFormat", it)
            set("minecraftMessageFormat", null)
            plugin.saveConfig()
        }

        getString("telegramMessageFormat")?.let {
            plugin.logger.warning(
                """
                Config option "telegramMessageFormat" is deprecated.
                Moved it to new key "minecraftFormat"
                """.trimIndent().replace('\n', ' ')
            )
            set("minecraftFormat", it)
            set("telegramMessageFormat", null)
            plugin.saveConfig()
        }

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
        enableIgnAuth = getBoolean("enableIgnAuth", false)

        botToken = getString("botToken") ?: throw Exception(C.WARN.noToken)
        allowWebhook = getBoolean("useWebhook", false)
        @Suppress("unchecked_cast")
        webhookConfig = get("webhookConfig") as Map<String, Any>?
        pollTimeout = getInt("pollTimeout", 30)

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
        // NB: Setting to null, if false, because API expects either `true` or absent parameter
        silentMessages = getBoolean("silentMessages").let { if (!it) null else true }
        apiOrigin = getString("apiOrigin", "https://api.telegram.org")!!
        debugHttp = getBoolean("debugHttp", false)
    }
}
