package org.kraftwerk28.spigot_tg_bridge

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel

interface Poller {
    suspend fun start(scope: CoroutineScope): ReceiveChannel<Update>
    suspend fun stop(scope: CoroutineScope)
}