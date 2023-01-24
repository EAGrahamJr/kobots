/*
 * Copyright 2022-2022 by E. A. Graham, Jr.
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

package crackers.kobots.utilities

import com.diozero.util.Hex
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Ignore exceptions, for those occasions where you truly don't care.
 */
fun <R : Any> ignoreErrors(block: () -> R?): Optional<R> =
    try {
        Optional.ofNullable(block())
    } catch (_: Throwable) {
        // ignored
        Optional.empty()
    }

/**
 * Elapsed time.
 */
fun Instant.elapsed() = Duration.between(this, Instant.now())

/**
 * Short form to get unsigned hex strings
 */
fun Int.hex() = Hex.encode(this)
fun Short.hex() = Hex.encode(this)
fun Byte.hex() = Hex.encode(this)

/**
 * Run with delay
 */
fun ScheduledExecutorService.withDelay(
    initialDelay: kotlin.time.Duration,
    delay: kotlin.time.Duration,
    execute: () -> Unit
) {
    scheduleWithFixedDelay(execute, initialDelay.inWholeNanoseconds, delay.inWholeNanoseconds, TimeUnit.NANOSECONDS)
}

/**
 * Run rate
 */
fun ScheduledExecutorService.withRate(
    initialDelay: kotlin.time.Duration,
    delay: kotlin.time.Duration,
    execute: () -> Unit
) {
    scheduleAtFixedRate(execute, initialDelay.inWholeNanoseconds, delay.inWholeNanoseconds, TimeUnit.NANOSECONDS)
}

/**
 * Captures data in a simple FIFO buffer for averaging. Probably not suitable for large sizes.
 */
class SimpleAverageMeasurement(val bucketSize: Int, val initialValue: Float = Float.MAX_VALUE) {
    private val dumbBuffer = CopyOnWriteArrayList<Float>()

    val value: Float
        get() = if (dumbBuffer.isEmpty()) initialValue else dumbBuffer.average().toFloat()

    operator fun plusAssign(v: Float) {
        dumbBuffer += v
        while (dumbBuffer.size > bucketSize) dumbBuffer.removeAt(0)
    }
}

fun microDuration(micros: Long) = Duration.ofNanos(micros * 1000)
