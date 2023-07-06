package org.kraftwerk28.spigot_tg_bridge

import kotlinx.coroutines.*
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds
import org.kraftwerk28.spigot_tg_bridge.Constants as C

class Configuration(plugin: Plugin) : YamlConfiguration() {
    val isEnabled: Boolean
        get() = getBoolean("isEnabled", true)
    val logFromMCtoTG: Boolean
        get() = getBoolean("logFromMCtoTG", true)
    val telegramFormat: String
        get() = getString("telegramFormat", "<i>%username%</i>: %message%")!!
    val minecraftFormat: String
        get() = getString("minecraftFormat", "<%username%>: %message%")!!
    val serverStartMessage: String?
        get() = getString("serverStartMessage")
    val serverStopMessage: String?
        get() = getString("serverStopMessage")
    val logJoinLeave: Boolean
        get() = getBoolean("logJoinLeave", false)
    val joinString: String
        get() = getString("strings.joined", "<i>%username%</i> joined.")!!
    val leaveString: String
        get() = getString("strings.left", "<i>%username%</i> left.")!!
    val logDeath: Boolean
        get() = getBoolean("logPlayerDeath", false)
    val logPlayerAsleep: Boolean
        get() = getBoolean("logPlayerAsleep", false)
    val onlineString: String
        get() = getString("strings.online", "Online")!!
    val nobodyOnlineString: String
        get() = getString("strings.nobodyOnline", "Nobody online")!!
    val enableIgnAuth: Boolean
        get() = getBoolean("enableIgnAuth", false)
    val silentMessages: Boolean?
        get() = getBoolean("silentMessages").let { if (!it) null else true }

    // Telegram bot stuff
    val botToken: String
        get() = getString("botToken") ?: throw Exception(C.WARN.noToken)
    val allowedChats: List<Long>
        get() = getLongList("chats")
    val logFromTGtoMC: Boolean
        get() = getBoolean("logFromTGtoMC", true)
    val allowWebhook: Boolean
        get() = getBoolean("useWebhook", false)
    val webhookConfig: Map<String, Any>?
        @Suppress("unchecked_cast")
        get() = get("webhookConfig") as Map<String, Any>?
    val pollTimeout: Int
        get() = getInt("pollTimeout", 30)
    val apiOrigin: String
        get() = getString("apiOrigin", "https://api.telegram.org")!!
    val debugHttp: Boolean
        get() = getBoolean("debugHttp", false)

    val commands = BotCommands(this)

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
                                plugin.restart()
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

        // Legacy convenience
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

        // Legacy convenience
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
    }
}
