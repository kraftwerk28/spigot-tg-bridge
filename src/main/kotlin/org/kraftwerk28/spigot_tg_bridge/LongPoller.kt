package org.kraftwerk28.spigot_tg_bridge

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

class LongPoller(
    private val bot: TgBot,
    private val pollTimeout: Int = 30,
) : Poller {
    private var currentOffset: Long = 0

    override suspend fun start(scope: CoroutineScope) = scope.produce {
        loop@ while (true) {
            try {
                val res = bot.api.getUpdates(offset = currentOffset, timeout = pollTimeout)
                res.result?.let { updates ->
                    updates.forEach { send(it) }
                    updates.lastOrNull()?.let { update ->
                        currentOffset = update.updateId + 1
                    }
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
    }

    override suspend fun stop(scope: CoroutineScope) {
    }
}
