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
import java.time.Duration

/**
 * A generic flow
 */
val theFlow: MutableSharedFlow<Any> =
    MutableSharedFlow(extraBufferCapacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST)

fun <V> registerPublisher(
    f: MutableSharedFlow<V>,
    pollInterval: Duration = Duration.ofMillis(100),
    eventProducer: () -> V
) {
    CoroutineScope(Dispatchers.Default).launch {
        // TODO this should be a system flag
        while (true) {
            delay(pollInterval.toMillis())
            withContext(Dispatchers.IO) { eventProducer().let { f.tryEmit(it) } }
        }
    }
}

/**
 * Consumer is responsible for filtering.
 */
fun <V> registerConsumer(f: Flow<V>, eventConsumer: (V) -> Unit) {
    f.onEach { message ->
        withContext(Dispatchers.IO) { eventConsumer(message) }
    }.launchIn(CoroutineScope(Dispatchers.Default))
}
