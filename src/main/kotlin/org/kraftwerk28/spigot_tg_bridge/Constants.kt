package org.kraftwerk28.spigot_tg_bridge

object Constants {
    const val configFilename = "config.yml"
    // Config field names
    object FIELDS {
        const val ENABLE = "enable"
        const val BOT_TOKEN = "botToken"
        const val BOT_USERNAME = "botUsername"
        const val ALLOWED_CHATS = "chats"
        const val LOG_FROM_MC_TO_TG = "logFromMCtoTG"
        const val LOG_FROM_TG_TO_MC = "logFromTGtoMC"
        const val SERVER_START_MSG = "serverStartMessage"
        const val SERVER_STOP_MSG = "serverStopMessage"
        const val LOG_JOIN_LEAVE = "logJoinLeave"
        const val LOG_PLAYER_DEATH = "logPlayerDeath"
        const val LOG_PLAYER_ASLEEP = "logPlayerAsleep"
        const val USE_WEBHOOK = "useWebhook"
        const val WEBHOOK_CONFIG = "webhookConfig"
        object STRINGS {
            const val ONLINE = "strings.online"
            const val OFFLINE = "strings.offline"
            const val JOINED = "strings.joined"
            const val LEFT = "strings.left"
        }
        object COMMANDS {
            const val TIME = "commands.time"
            const val ONLINE = "commands.online"
        }
        const val TELEGRAM_MESSAGE_FORMAT = "telegramMessageFormat"
    }
    object DEFS {
        const val logFromMCtoTG = false
        const val logFromTGtoMC = false
        const val logJoinLeave = false
        const val logPlayerDeath = false
        const val logPlayerAsleep = false
        object COMMANDS {
            const val TIME = "time"
            const val ONLINE = "online"
        }
        const val playersOnline = "Online"
        const val nobodyOnline = "Nobody online"
        const val playerJoined = "joined"
        const val playerLeft = "left"
        const val useWebhook = false
        const val enable = true
        const val telegramMessageFormat = "<%username%>: %message%"
    }
    object WARN {
        const val noConfigWarning = "No config file found! Writing default config to config.yml."
        const val noToken = "Bot token must be defined."
        const val noUsername = "Bot username must be defined."
    }
    object TIMES_OF_DAY {
        const val day = "\uD83C\uDFDE Day"
        const val sunset = "\uD83C\uDF06 Sunset"
        const val night = "\uD83C\uDF03 Night"
        const val sunrise = "\uD83C\uDF05 Sunrise"
    }
    const val USERNAME_PLACEHOLDER = "%username%"
    const val MESSAGE_TEXT_PLACEHOLDER = "%message%"
}
