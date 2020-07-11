package org.kraftwerk28.spigot_tg_bridge

object Constants {
    const val configFilename = "config.yml"
    // Config field names
    object FIELDS {
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
    }
    object WARN {
        const val noConfigWarning = "No config file found! Writing default config to config.yml."
        const val noToken = "Bot token must be defined."
        const val noUsername = "Bot username must be defined."
    }
}
