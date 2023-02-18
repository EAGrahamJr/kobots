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
import java.awt.Color
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

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

/**
 * Note this only extracts the HSB _hue_ component of the color.
 */
fun colorInterval(from: Color, to: Color, steps: Int): List<Color> =
    colorIntervalFromHSB(
        Color.RGBtoHSB(from.red, from.green, from.blue, null)[0] * 360f,
        Color.RGBtoHSB(to.red, to.green, to.blue, null)[0] * 360f,
        steps
    )

/**
 * Generate a range of [n] colors based on the HSB hue (0==red) [angleFrom] to [angleTo]
 */
fun colorIntervalFromHSB(angleFrom: Float, angleTo: Float, n: Int): List<Color> {
    val angleRange = angleTo - angleFrom
    val stepAngle = angleRange / n
    val colors = mutableListOf<Color>()
    for (i in 0 until n) {
        val angle = angleFrom + i * stepAngle
        colors += Color.getHSBColor(angle / 360f, 1f, 1f)
    }
    return colors
}
