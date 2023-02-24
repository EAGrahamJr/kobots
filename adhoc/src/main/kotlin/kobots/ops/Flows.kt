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

package kobots.ops

import com.diozero.sbc.LocalSystemInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * A generic flow.
 */
val theFlow: MutableSharedFlow<Any> = createEventBus()

// TODO wrap this in another class so it doesn't look like we're using it?
inline fun <reified R> createEventBus(capacity: Int = 5): MutableSharedFlow<R> =
    MutableSharedFlow(extraBufferCapacity = capacity, onBufferOverflow = BufferOverflow.DROP_OLDEST)

/**
 * A default error handler that only logs.
 */
private val errorHandlerLogger = LoggerFactory.getLogger("Default EventBus ErrorHandler")
private val DEFAULT_HANDLER: (t: Throwable) -> Unit = { t -> errorHandlerLogger.error("Unhandled error", t) }
private fun withErrorHandler(handler: (t: Throwable) -> Unit, block: () -> Unit) {
    try {
        block()
    } catch (t: Throwable) {
        handler(t)
    }
}

/**
 * A flow-event publisher for **blocking** operations: calls the [eventProducer] every [pollInterval] and uses the
 * `tryEmit` to publish to the flow. If an error occurs, the [errorHandler] is invoked with it. Errors will **not**
 * stop the polling cycle, just in case the device becomes available again.
 *
 * Note that since this runs in a coroutine, the timing on the poll cycle is not likely to be exact.
 */
fun <V> MutableSharedFlow<V>.registerPublisher(
    pollInterval: Duration = Duration.ofMillis(100),
    errorHandler: (t: Throwable) -> Unit = DEFAULT_HANDLER,
    eventProducer: () -> V
) {
    CoroutineScope(Dispatchers.Default).launch {
        // TODO this should be a system flag?
        while (true) {
            delay(pollInterval.toMillis())
            withContext(Dispatchers.IO) {
                withErrorHandler(errorHandler) { eventProducer().let { tryEmit(it) } }
            }
        }
    }
}

/**
 * A flow event consumer for **blocking** operations. When a message arrives, it is passed to the [eventConsumer]. If
 * an error occurs, the [errorHandler] is invoked with it.
 */
fun <V> Flow<V>.registerConsumer(errorHandler: (t: Throwable) -> Unit = DEFAULT_HANDLER, eventConsumer: (V) -> Unit) {
    onEach { message ->
        withContext(Dispatchers.IO) {
            withErrorHandler(errorHandler) { eventConsumer(message) }
        }
    }.launchIn(CoroutineScope(Dispatchers.Default))
}

/**
 * A flow and producer specifically for the platform's CPU.
 */
private val cpuBus by lazy {
    createEventBus<Double>().also {
        it.registerPublisher(Duration.ofMillis(500)) {
            LocalSystemInfo.getInstance().getCpuTemperature().toDouble()
        }
    }
}

/**
 * Add a consumer to get the CPU temperature.
 */
fun registerCPUTempConsumer(errorHandler: (t: Throwable) -> Unit = DEFAULT_HANDLER, eventConsumer: (Double) -> Unit) {
    cpuBus.registerConsumer(errorHandler, eventConsumer)
}
