/*
 * Copyright 2022-2023 by E. A. Graham, Jr.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package crackers.kobots.ops

import com.diozero.sbc.LocalSystemInfo
import crackers.kobots.ops.KobotsEventBus.Companion.NOOP_HANDLER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Predicate
import kotlin.concurrent.thread

/**
 * Completely prototype code for using coroutine multiflows as an asynchronous event bus.
 *
 * **NOTE** Expander boards/micro-controllers may not play nice, so the whole controller may need to be wrapped in
 * its own "thread pool" to change the state of a lot of things.
 */
const val MAX_SCHEDULER = "kobots.max.scheduler.threads"

private val scheduler: ScheduledExecutorService by lazy {
    Executors.newScheduledThreadPool(System.getProperty(MAX_SCHEDULER, "5").toInt()).also { svc ->
        Runtime.getRuntime().addShutdownHook(thread(start = false) { svc.shutdownNow() })
    }
}

private val busRegistry by lazy {
    Runtime.getRuntime().addShutdownHook(thread(start = false) { stopTheBus() })
    CopyOnWriteArraySet<KobotsEventBus<*>>()
}

fun stopTheBus() {
    busRegistry.forEach { it.close() }
    scheduler.shutdownNow()
}

class KobotsEventBus<V>(capacity: Int, name: String? = null) : AutoCloseable {
    val logger = LoggerFactory.getLogger(name ?: this::class.java.simpleName)
    private val flow = MutableSharedFlow<V>(
        extraBufferCapacity = capacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val busRunning = AtomicBoolean(true)

    /**
     * Expose a shared flow for anyone that wants it.
     */
    val sharedFlow: SharedFlow<V> = flow.asSharedFlow()

    /**
     * Default handler that logs the errors with stack-traces and continues.
     */
    val defaultErrorHandler: (t: Throwable) -> Unit by lazy { { t -> logger.error("Unhandled error", t) } }

    /**
     * Error handler that simply logs the error messages and continues.
     */
    val errorMessageHandler: (t: Throwable) -> Unit by lazy {
        { t -> logger.error("Unhandled error: ${t.localizedMessage}") }
    }

    init {
        if (busRegistry.contains(this)) throw IllegalStateException("Bus cannot be instantiated multiple times")
        busRegistry += this
        Runtime.getRuntime().addShutdownHook(thread(start = false) { busRunning.set(false) })
    }

    override fun close() {
        logger.warn("bus closed")
        busRunning.set(false)
        busRegistry -= this
    }

    /**
     * A flow-event publisher for **blocking** operations: calls the [eventProducer] every [pollInterval] and uses the
     * `tryEmit` to publish to the flow. If an error occurs, the [errorHandler] is invoked with it. Errors will **not**
     * stop the polling cycle, just in case the device becomes available again.
     *
     * Each pubsliher is running in a separate thread.
     */
    fun registerPublisher(
        pollInterval: Duration = DEFAULT_POLL_INTERVAL,
        errorHandler: (t: Throwable) -> Unit = defaultErrorHandler,
        eventProducer: () -> V
    ) {
        scheduler.scheduleAtFixedRate(
            Runnable {
                if (busRunning.get()) withErrorHandler(errorHandler) { eventProducer().let { flow.tryEmit(it) } }
            },
            pollInterval.toNanos(),
            pollInterval.toNanos(),
            TimeUnit.NANOSECONDS
        )
    }

    /**
     * A flow event consumer for **blocking** operations. When a message arrives, it is passed to the [eventConsumer].
     * The [errorHandler] is invoked with any errors that occur.
     */
    fun registerConsumer(errorHandler: (t: Throwable) -> Unit = defaultErrorHandler, eventConsumer: (V) -> Unit) {
        registerConditionalConsumer(errorHandler, { true }, eventConsumer)
    }

    /**
     * A flow event consumer for **blocking** operations. When a message arrives, it is evaluated against the
     * [messageCondition] and, if accepted, passed to the [eventConsumer]. The [errorHandler] is invoked with any
     * errors that occur.
     */
    fun registerConditionalConsumer(
        errorHandler: (t: Throwable) -> Unit = defaultErrorHandler,
        messageCondition: Predicate<V>,
        eventConsumer: (V) -> Unit
    ) {
        sharedFlow.onEach { message ->
            if (messageCondition.test(message) && busRunning.get()) {
                withContext(Dispatchers.IO) {
                    withErrorHandler(errorHandler) { eventConsumer(message) }
                }
            }
        }.launchIn(CoroutineScope(Dispatchers.Default))
    }

    /**
     * A default error handler that only logs.
     */
    private fun withErrorHandler(handler: (t: Throwable) -> Unit, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            handler(t)
        }
    }

    companion object {
        /**
         * No-op handler: ignores errors.
         */
        val NOOP_HANDLER: (t: Throwable) -> Unit = {}

        /**
         * Default polling interval for producers.
         */
        val DEFAULT_POLL_INTERVAL = Duration.ofMillis(100)
    }
}

/**
 * A generic flow.
 */
val theBus: KobotsEventBus<Any> = createEventBus()

inline fun <reified R> createEventBus(capacity: Int = 5, name: String? = null) = KobotsEventBus<R>(capacity, name)

/**
 * A flow and producer specifically for the platform's CPU.
 */
private val cpuBus by lazy {
    createEventBus<Double>(name = "CPUTemp").apply {
        registerPublisher(Duration.ofMillis(500)) {
            LocalSystemInfo.getInstance().cpuTemperature.toDouble()
        }
    }
}

/**
 * Add a consumer to get the CPU temperature.
 */
fun registerCPUTempConsumer(errorHandler: (t: Throwable) -> Unit = NOOP_HANDLER, eventConsumer: (Double) -> Unit) {
    cpuBus.registerConsumer(errorHandler, eventConsumer)
}
