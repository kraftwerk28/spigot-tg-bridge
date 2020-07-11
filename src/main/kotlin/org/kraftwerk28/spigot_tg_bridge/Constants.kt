package org.kraftwerk28.spigot_tg_bridge

object Constants {
    val configFilename = "config.yml"
    // Config field names
    object FIELDS {
        val BOT_TOKEN = "botToken"
        val BOT_USERNAME = "botUsername"
        val ALLOWED_CHATS = "chats"
        val LOG_FROM_MC_TO_TG = "logFromMCtoTG"
        val LOG_FROM_TG_TO_MC = "logFromTGtoMC"
        val SERVER_START_MSG = "serverStartMessage"
        val SERVER_STOP_MSG = "serverStopMessage"
        val LOG_JOIN_LEAVE = "logJoinLeave"
        val LOG_PLAYER_DEATH = "logPlayerDeath"
        val LOG_PLAYER_ASLEEP = "logPlayerAsleep"
        object STRINGS {
            val ONLINE = "strings.online"
            val OFFLINE = "strings.offline"
            val JOINED = "strings.joined"
            val LEFT = "strings.left"
        }
        object COMMANDS {
            val TIME = "commands.time"
            val ONLINE = "commands.online"
        }
    }
    object DEFS {
        val logFromMCtoTG = false
        val logFromTGtoMC = false
        val logJoinLeave = false
        val logPlayerDeath = false
        val logPlayerAsleep = false
        object COMMANDS {
            val TIME = "/time"
            val ONLINE = "/online"
        }
        val playersOnline = "Online"
        val nobodyOnline = "Nobody online"
        val playerJoined = "joined"
        val playerLeft = "left"
    }
    object WARN {
        val noConfigWarning = "No config file found! Writing default config to config.yml."
        val noToken = "Bot token must be defined."
        val noUsername = "Bot username must be defined."
    }
}
