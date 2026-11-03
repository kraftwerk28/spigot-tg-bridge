package eu.pablob.paper_telegram_bridge

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runInterruptible
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds
import eu.pablob.paper_telegram_bridge.Constants as C

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
    val logPlayerAdvancement: Boolean
    val logInventory: Boolean
    val logWhitelistKick: Boolean
    val onlineString: String
    val nobodyOnlineString: String
    val silentMessages: Boolean?
    val advancementString: String
    val goalString: String
    val challengeString: String
    val deathString: String
    val mediaMessagePhotoString: String
    val mediaMessageVideoString: String
    val mediaMessageAudioString: String
    val mediaMessageVoiceString: String
    val mediaMessageVideoNoteString: String


    // Telegram bot stuff
    val botToken: String
    val allowedChats: List<Long>
    val allowedTopics: List<Long>
    val admins:  List<Long>
    val logFromTGtoMC: Boolean
    private val allowWebhook: Boolean
    private val webhookConfig: Map<String, Any>?
    val pollTimeout: Int
    val apiOrigin: String
    val debugHttp: Boolean
    val escapeEmojis: Boolean

    var commands: BotCommands

    init {
        val cfgFile = File(plugin.dataFolder, C.CONFIG_FILENAME)
        if (!cfgFile.exists()) {
            cfgFile.parentFile.mkdirs()
            plugin.saveDefaultConfig()
            // plugin.saveResource(C.configFilename, false);
            throw Exception(C.WARN.NO_CONFIG_WARNING)
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
            } catch (_: Exception) {
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
        allowedTopics = getLongList("topics")

        botToken = getString("botToken") ?: throw Exception(C.WARN.NO_TOKEN)
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
        logPlayerAdvancement = getBoolean("logPlayerAdvancement", false)
        logInventory = getBoolean("logInventory", false)
        logWhitelistKick = getBoolean("logWhitelistKick", true)
        advancementString =
            getString(
                "strings.advancement",
                "<i>%username%</i> has made the advancement <b>%advancement%</b>.\n" +
                        "(<i>%description%</i>)"
            )!!
        goalString =
            getString(
                "strings.goal", "<i>%username%</i> has reached the goal <b>%advancement%</b>.\n" +
                        "(<i>%description%</i>)"
            )!!
        challengeString =
            getString(
                "strings.challenge", "<i>%username%</i> has completed the challenge <b>%advancement%</b>.\n" +
                        "(<i>%description%</i>)"
            )!!
        deathString = getString("strings.death", "%deathMessage%")!!
        mediaMessagePhotoString = getString("strings.mediaMessage.photo", "[📸 Photo]")!!
        mediaMessageVideoString = getString("strings.mediaMessage.video", "[🎥 Video]")!!
        mediaMessageAudioString = getString("strings.mediaMessage.audio", "[🎵 Audio]")!!
        mediaMessageVoiceString = getString("strings.mediaMessage.voice", "[🎤 Voice]")!!
        mediaMessageVideoNoteString = getString("strings.mediaMessage.video_note", "[🎥 Video Note]")!!
        commands = BotCommands(this)
        // NB: Setting to null, if false, because API expects either `true` or absent parameter
        silentMessages = getBoolean("silentMessages").let { if (!it) null else true }
        apiOrigin = getString("apiOrigin", "https://api.telegram.org")!!
        debugHttp = getBoolean("debugHttp", false)
        escapeEmojis = getBoolean("escapeEmojis", true)
        admins = getLongList("admins")
    }
}
