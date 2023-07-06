package org.kraftwerk28.spigot_tg_bridge

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.URL

class WebhookPoller(
    private val bot: TgBot,
    private val webhookUrl: String,
    private val serverPort: Int,
) : Poller {
    private var server: HttpServer? = null

    override suspend fun start(scope: CoroutineScope) = scope.produce {
        server = HttpServer.create(InetSocketAddress(serverPort), 0).apply {
            val hookPath = URL(webhookUrl).path
            createContext(hookPath) { t ->
                scope.launch {
                    t?.requestBody?.let {
                        val reader = InputStreamReader(it)
                        val json = JsonParser.parseReader(reader)
                        Gson().fromJson(json, Update::class.java)?.let { update ->
                            send(update)
                        }
                    }
                }
            }
        }
        bot.api.setWebhook(url = webhookUrl)
    }

    override suspend fun stop(scope: CoroutineScope) {
        server?.run { stop(0) }
    }
}