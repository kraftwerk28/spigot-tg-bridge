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
|:-----:|:------------|:----:|:--------:|:-------:|
| enable | If plugin should be enabled | `boolean` | :x: | `true` |
| botToken | Telegram bot token ([How to create bot](https://core.telegram.org/bots#3-how-do-i-create-a-bot)) | `string` | :heavy_check_mark: | - |
| botUsername | Telegram bot username | `string` | :heavy_check_mark: | - |
| chats | Chats, where bot will work (to prevent using bot by unknown chats) | `number[] or string[]` | :heavy_check_mark: | `[]` |
| serverStartMessage | What will be sent to chats when server starts | `string` | :x: | `'Server started.'` |
| serverStopMessage | What will be sent to chats when server stops | `string` | :x: | `'Server stopped.'` |
| logJoinLeave | If true, plugin will send corresponding messages to chats, when player joins or leaves server | `boolean` | :x: | `true` |
| logFromMCtoTG | If true, plugin will send messages from players on server, to Telegram chats | `boolean` | :x: | `true` |
| logFromTGtoMC | If true, plugin will send messages from chats, to Minecraft server | `boolean` | :x: | `true` |
| logPlayerDeath | If true, plugin will send message to Telegram if player died | `boolean` | :x: | `false` |
| logPlayerAsleep | If true, plugin will send message to Telegram if player fell asleep | `boolean` | :x: | `false` |
| strings | Dictionary of tokens - strings for plugin i18n | `Map<string, string>` | :x: | See default config |
| commands | Dictionary of command text used in Telegram bot | `Map<string, string>` | :x: | See default config |
| telegramMessageFormat | Format string for TGtoMC chat message | `string` | :x: | See default config |

## Telegram bot commands:

Commands are customizeable through config, but there are default values for them as well

| Command | Description |
|:-------:|:------------|
| `/online` | Get players, currently online |
| `/time`   | Get [time](https://minecraft.gamepedia.com/Day-night_cycle) on server |

## Format string:
Must contain `%username%` and `%message` inside.
You can customize message color with it. See [message color codes](https://www.digminecraft.com/lists/color_list_pc.php).

P. S.: related to [this issue](https://github.com/kraftwerk28/spigot-tg-bridge/issues/6)

## Plugin commands:

| Command | Description |
|:-------:|:------------|
| `/tgbridge_reload` | Reload plugin configuration w/o need to stop the server. Works only through server console |
