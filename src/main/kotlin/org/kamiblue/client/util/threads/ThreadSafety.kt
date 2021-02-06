package org.kamiblue.client.util.threads

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.kamiblue.client.event.ClientEvent
import org.kamiblue.client.event.ClientExecuteEvent
import org.kamiblue.client.event.SafeClientEvent
import org.kamiblue.client.event.SafeExecuteEvent
import org.kamiblue.event.ListenerManager
import org.kamiblue.event.listener.AsyncListener
import org.kamiblue.event.listener.DEFAULT_PRIORITY
import org.kamiblue.event.listener.Listener

inline fun <reified T : Any> Any.safeAsyncListener(noinline function: suspend SafeClientEvent.(T) -> Unit) {
    ListenerManager.register(this, AsyncListener(this, T::class.java) { runSafeSuspend { function(it) } })
}

inline fun <reified T : Any> Any.safeListener(priority: Int = DEFAULT_PRIORITY, noinline function: SafeClientEvent.(T) -> Unit) {
    ListenerManager.register(this, Listener(this, T::class.java, priority) { runSafe { function(it) } })
}

fun ClientEvent.toSafe() =
    if (world != null && player != null && playerController != null && connection != null) SafeClientEvent(world, player, playerController, connection)
    else null

fun ClientExecuteEvent.toSafe() =
    if (world != null && player != null && playerController != null && connection != null) SafeExecuteEvent(world, player, playerController, connection, this)
    else null

fun runSafe(block: SafeClientEvent.() -> Unit) {
    ClientEvent().toSafe()?.let { block(it) }
}

fun <R> runSafeR(block: SafeClientEvent.() -> R): R? {
    return ClientEvent().toSafe()?.let { block(it) }
}

suspend fun <R> runSafeSuspend(block: suspend SafeClientEvent.() -> R): R? {
    return ClientEvent().toSafe()?.let { block(it) }
}

/**
 * Runs [block] on Minecraft main thread (Client thread)
 * The [block] will the called with a [SafeClientEvent] to ensure null safety.
 *
 * @return [CompletableDeferred] callback
 *
 * @see [onMainThread]
 */
suspend fun <T> onMainThreadSafe(block: suspend SafeClientEvent.() -> T) =
    onMainThread { ClientEvent().toSafe()?.block() }

/**
 * Runs [block] on Minecraft main thread (Client thread)
 *
 * @return [CompletableDeferred] callback
 *
 * @see [onMainThreadSafe]
 */
suspend fun <T> onMainThread(block: suspend () -> T) =
    MainThreadExecutor.add(block)