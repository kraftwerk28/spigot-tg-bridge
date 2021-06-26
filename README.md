# Spigot <-> Telegram bridge plugin

### This plugin will send chat messages from Minecraft to Telegram, and from Telegram to Minecraft.

## How to use:

1. Download .jar file from [releases page](https://github.com/kraftwerk28/spigot-tg-bridge/releases), and put it in `plugins/` directory on your server **OR** clone this repo and run `gradle` inside repo's directory.

2. If you already have telegram bot, skip this step. Otherwise create it through [BotFather](https://t.me/BotFather). You'll go through step-by-step instructions, give a bot __username__ and most importantly, obtain a bot __token__. Save this token for future use. **Note:** in order to make your bot hear raw text messages (not commands), you must disable [privacy mode](https://core.telegram.org/bots#privacy-mode) option which is on by default. Go through bot's settings: **Bot Settings -> Group Privacy** and click **Turn Off**.

3. Next, you need to tell plugin about your new bot. You can either:
    - Run Spigot server, plugin will log `"No config file found! Saving default one."`. After that, stop server and proceed to 4th step.
    - Copy [config.yml](https://raw.githubusercontent.com/kraftwerk28/spigot-tg-bridge/master/src/main/resources/config.yml) to `plugins/SpigotTGBridge/` in your server directory.

4. A `config.yml` is just a [valid YAML](https://en.wikipedia.org/wiki/YAML) file, alternative for JSON, but more human-readable.
   Now, take bot's __token__ which you got in 2nd step and paste them into `config.yml`, so it looks like this:
   ```yaml
   botToken: abcdefghijklmnopq123123123
   # other configuration values...
   ```

5. Run spigot server.

6. Add you bot to chats, where you plan to use it. In each of them, run `/chat_id` command. The bot should respond and give special value - __chat id__. Now, open `config.yml` and paste this ID under `chats` section, so it will look like this:
    ```yaml
    botToken: abcdefghijklmnopq123123123
    chats:
      # Note about doubling minus sign. This is not a mistake, first one means list element, the second one - actual minus
      - -123456789
      - 987654321
      # other chat id's...
    ```

7. You can extend `config.yml` with more tweaks, which are described in the table below, but it's not nesessary, plugin will use default values instead, if they're missing. Also, check out the [example](src/main/resources/config.yml).

8. Re-run server or type `tgbridge_reload` into server's console.


## Plugin configuration:

| Field | Description | Type | Required | Default |
|:-----:|:------------|:----:|:--------:|:-------:|
| enable | If plugin should be enabled | `boolean` | :x: | `true` |
| botToken | Telegram bot token ([How to create bot](https://core.telegram.org/bots#3-how-do-i-create-a-bot)) | `string` | :heavy_check_mark: | - |
| chats | Chats, where bot will work (to prevent using bot by unknown chats) | `number[] or string[]` | :heavy_check_mark: | `[]` |
| serverStartMessage | What will be sent to chats when server starts | `string` | :x: | `'Server started.'` |
| serverStopMessage | What will be sent to chats when server stops | `string` | :x: | `'Server stopped.'` |
| logJoinLeave | If true, plugin will send corresponding messages to chats, when player joins or leaves server | `boolean` | :x: | `true` |
| logFromMCtoTG | If true, plugin will send messages from players on server, to Telegram chats | `boolean` | :x: | `true` |
| logFromTGtoMC | If true, plugin will send messages from chats, to Minecraft server | `boolean` | :x: | `true` |
| logPlayerDeath | If true, plugin will send message to Telegram if player died | `boolean` | :x: | `false` |
| logPlayerAsleep | If true, plugin will send message to Telegram if player fell asleep | `boolean` | :x: | `false` |
| strings | Dictionary of tokens - strings for plugin i18n | `Map<string, string>` | :x: | See default config |
| commands | Dictionary of command text used in Telegram bot | `Map<string, string>` | :heavy_check_mark: | See below |
| telegramMessageFormat | Format string for TGtoMC chat message | `string` | :x: | See default config |
| minecraftMessageFormat | Format string for MCtoTG chat message | `string` | :x: | See default config |


## Telegram bot commands:

Commands are customizeable through config. If command doesn't exist in config, it will be disabled

| Command | Description |
|:-------:|:------------|
| `/online` | Get players, currently online |
| `/time`   | Get [time](https://minecraft.gamepedia.com/Day-night_cycle) on server |
| `/chat_id`   | Get current chat ID (in which command was run) for config.yml |


## Format string:

```
+--------+ >--minecraftMessageFormat(message)-> +--------------+
| Spigot |                                      | Telegram bot |
+--------+ <--telegramMessageFormat(message)--< +--------------+
```

Applies to `telegramMessageFormat` and `minecraftMessageFormat` configurations.
Must contain `%username%` and `%message%` inside.
You can customize message color with it (coloring works only for `telegramMessageFormat`). You can customize bold/italics/stirethrough formatting (works only for `minecraftMessageFormat`). See [Minecraft message color codes](https://www.digminecraft.com/lists/color_list_pc.php) and [Telegram message formatting](https://core.telegram.org/bots/api#html-style) for more information.
This feature is related to [this issue](https://github.com/kraftwerk28/spigot-tg-bridge/issues/6)


## Plugin commands:

| Command | Description |
|:-------:|:------------|
| `tgbridge_reload` | Reload plugin configuration w/o need to stop the server. Works only through server console |
