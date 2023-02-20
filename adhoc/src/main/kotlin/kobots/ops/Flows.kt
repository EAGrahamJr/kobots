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

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration

/**
 * A generic flow
 */
val theFlow: MutableSharedFlow<Any> = createEventBus()

// TODO wrap this in another class so it doesn't look like we're using it?
inline fun <reified R> createEventBus(capacity: Int = 5): MutableSharedFlow<R> =
    MutableSharedFlow(extraBufferCapacity = capacity, onBufferOverflow = BufferOverflow.DROP_OLDEST)

/**
 * A default error handler that does nothing.
 */
private val DEFAULT_HANDLER: (t: Throwable) -> Unit = {}

/**
 * A flow-event publisher for **blocking** operations: calls the [eventProducer] every [pollInterval] and uses the
 * `tryEmit` to publish to the flow. If an error occurs, the [errorHandler] is invoked with it.
 */
fun <V> MutableSharedFlow<V>.registerPublisher(
    pollInterval: Duration = Duration.ofMillis(100),
    errorHandler: (t: Throwable) -> Unit = DEFAULT_HANDLER,
    eventProducer: () -> V
) {
    CoroutineScope(Dispatchers.Default).launch {
        // TODO this should be a system flag
        while (true) {
            delay(pollInterval.toMillis())
            withContext(Dispatchers.IO) {
                try {
                    eventProducer().let { tryEmit(it) }
                } catch (t: Throwable) {
                    errorHandler(t)
                }
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
            try {
                eventConsumer(message)
            } catch (t: Throwable) {
                errorHandler(t)
            }
        }
    }.launchIn(CoroutineScope(Dispatchers.Default))
}

/**
 * A flow and producer specifically for the platform's CPU
 */
private val cpuBus by lazy {
    createEventBus<Double>().also {
        it.registerPublisher(Duration.ofSeconds(1)) {
            Files.readAllLines(Paths.get("/sys/class/thermal/thermal_zone0/temp")).first().toDouble() / 1000
        }
    }
}

fun registerCPUTempConsumer(errorHandler: (t: Throwable) -> Unit = DEFAULT_HANDLER, eventConsumer: (Double) -> Unit) {
    cpuBus.onEach { message ->
        withContext(Dispatchers.IO) {
            try {
                eventConsumer(message)
            } catch (t: Throwable) {
                errorHandler(t)
            }
        }
    }.launchIn(CoroutineScope(Dispatchers.Default))
}
