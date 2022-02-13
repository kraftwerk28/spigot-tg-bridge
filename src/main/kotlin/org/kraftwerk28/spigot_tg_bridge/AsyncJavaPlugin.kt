package org.kraftwerk28.spigot_tg_bridge

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
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

    override fun onLoad() {
        runBlocking { onLoadAsync() }
    }

    open suspend fun onEnableAsync() = Unit

    open suspend fun onDisableAsync() = Unit

    open suspend fun onLoadAsync() = Unit

    fun <T> launch(f: suspend () -> T) = scope.launch { f() }
}
