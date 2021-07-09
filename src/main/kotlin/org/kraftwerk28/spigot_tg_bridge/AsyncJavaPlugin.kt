package org.kraftwerk28.spigot_tg_bridge

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.java.JavaPlugin

open class AsyncJavaPlugin : JavaPlugin() {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val jobs: MutableList<Job> = mutableListOf()

    override fun onEnable() {
        runBlocking { onEnableAsync() }
    }

    override fun onDisable() {
        runBlocking {
            onDisableAsync()
            jobs.joinAll()
        }
    }

    open suspend fun onEnableAsync() = Unit

    open suspend fun onDisableAsync() = Unit

    fun <T> launch(f: suspend () -> T) = scope.launch {
        f()
    }.also {
        jobs.add(it)
    }
}
