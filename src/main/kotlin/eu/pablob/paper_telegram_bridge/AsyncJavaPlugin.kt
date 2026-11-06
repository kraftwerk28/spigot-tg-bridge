package eu.pablob.paper_telegram_bridge

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.java.JavaPlugin

open class AsyncJavaPlugin : JavaPlugin() {
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onEnable() {
        runBlocking { onEnableAsync() }
    }

    override fun onDisable() {
        runBlocking {
            onDisableAsync()
            scope.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    open suspend fun onEnableAsync() = Unit

    open suspend fun onDisableAsync() = Unit

    fun <T> launch(f: suspend () -> T) = scope.launch { f() }
}
