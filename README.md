# Spigot <-> Telegram bridge plugin

### This plugin will send chat messages from Minecraft to Telegram, and from Telegram to Minecraft.

## How to use:

1. Download .jar file from [releases page](https://github.com/kraftwerk28/spigot-tg-bridge/releases), and put it in `plugins/` directory on your server **OR** clone this repo and run `gradle shadowJar` inside repo's directory

2. Choose one of below to configure plugin:
    1. Run Spigot server, plugin will log `"No config file found! Saving default one."`. After that, stop server and proceed to 3rd step.
    
    **OR**
    2. Copy [config.yml](https://raw.githubusercontent.com/kraftwerk28/spigot-tg-bridge/master/src/main/resources/config.yml) to `plugins/SpigotTGBridge/` in your server directory.

3. Modify `plugins/SpigotTGBridge/config.yml` with text editor, where each field is described in table below.

4. Run server.

#### config.yml:
| Field | Description | Type | Required | Default |
| --- | --- | --- | --- | --- |
| botToken | Telegram bot token ([How to create bot](https://core.telegram.org/bots#3-how-do-i-create-a-bot)) | string | :heavy_check_mark: | - |
| botUsername | Telegram bot username | string | :heavy_check_mark: | - |
| chats | Chats, where bot will work (to prevent using bot by unknown chats) | number[] | :heavy_check_mark: | [] |
| serverStartMessage | What will be sent to chats when server starts | string | :x: | 'Server started.' |
| serverStopMessage | What will be sent to chats when server stops | string | :x: | 'Server stopped.' |
| logJoinLeave | If true, plugin will send corresponding messages to chats, when player joins or leaves server | boolean | :x: | true |
| logFromMCtoTG | If true, plugin will send messages from players on server, to Telegram chats | boolean | :x: | true |
| logFromTGtoMC | If true, plugin will send messages from chats, to Minecraft server | boolean | :x: | true |
| strings | Dictionary of tokens - strings for plugin i18n | Map<string, string> | :x: | See default config |

## Commands:
| Command | description |
| --- | --- |
| `/online` | Get players, currently online |
| `/time` | Get [time](https://minecraft.gamepedia.com/Day-night_cycle) on server |
