package org.kraftwerk28.spigot_tg_bridge

import kotlinx.coroutines.*
import org.bukkit.plugin.java.JavaPlugin
import kotlin.coroutines.CoroutineContext

open class AsyncJavaPlugin : JavaPlugin() {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val scope2 = CoroutineScope()

//    private val masterScope = CoroutineScope(Dispatchers.Default)
//    private var workerJob: Job? = null
//    private var disableJob: Job? = null

    override fun onEnable() {
        masterScope.launch {
            disableJob?.run { join() }
            workerJob = launch {
            }
            workerJob = Job().also { this@AsyncJavaPlugin.coroutineContext = it + Dispatchers.Default }
            withContext(this@AsyncJavaPlugin.coroutineContext) {
                logger.info("onEnableAsync before")
                onEnableAsync()
                logger.info("onEnableAsync after")
            }
        }
    }

    override fun onDisable() {
        masterScope.launch {
            try {
                withContext(this@AsyncJavaPlugin.coroutineContext) {
                    logger.info("onDisableAsync before")
                    onDisableAsync()
                    logger.info("onDisableAsync after")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            workerJob?.run {
                logger.info("Cancelling job")
                cancelAndJoin()
                logger.info("Job cancelled")
            }
            workerJob = null
            this@AsyncJavaPlugin.coroutineContext = Dispatchers.Default
            disableJob = null
        }.also {
            disableJob = it
        }
    }

    fun restart() {
        onDisable()
        onEnable()
    }

    open suspend fun onEnableAsync() = Unit

    open suspend fun onDisableAsync() = Unit

    fun launch(f: suspend CoroutineScope.() -> Unit) = scope.launch(block = f)
}
