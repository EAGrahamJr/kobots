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

package crackers.kobots.devices

import com.diozero.devices.sandpit.motor.StepperMotorInterface
import com.diozero.devices.sandpit.motor.StepperMotorInterface.Direction
import com.diozero.util.SleepUtil
import java.io.Serializable
import java.time.Duration
import kotlin.experimental.and

/**
 * Convert a float representing degrees C to F.
 */
fun Float.asDegreesF(): Float = (this * 9f / 5f) + 32.0f

/**
 * Checks to see if an Int is in the specified range
 */
fun Int.inRange(name: String, range: IntRange) =
    if (this !in range) throw IllegalArgumentException("'$name' value '$this' is out of bounds ($range)") else this

/**
 * Convert a boolean to integer "equivalent"
 */
fun Boolean.toInt() = if (this) 1 else 0

/**
 * Four things.
 */
data class Quadruple<out A, out B, out C, out D>(val first: A, val second: B, val third: C, val fourth: D) :
    Serializable {
    override fun toString(): String = "($first, $second, $third, $fourth)"
}

/**
 * Convert to two bytes
 */
fun Int.to2Bytes(): Pair<Byte, Byte> = toShort().toBytes()

/**
 * Convert to two bytes
 */
fun Short.toBytes(): Pair<Byte, Byte> {
    val hiByte = this.toUInt() shr 8
    val loByte = this and 0xFF
    // TODO big endian fs little endian (this is big endian
    return Pair(hiByte.toByte(), loByte.toByte())
}

/**
 * Common I2C pattern: two bytes and a buffer (register, offset, payload)
 */
fun twoBytesAndBuffer(first: Byte, second: Byte, buffer: ByteArray) = ByteArray(buffer.size + 2) {
    when (it) {
        0 -> first
        1 -> second
        else -> buffer[it - 2]
    }
}

/**
 * Rotate with a simple interruption - note this **can** be bad if the interruption method is left as the default.
 */
fun StepperMotorInterface.rotate(
    degrees: Float,
    direction: Direction = Direction.FORWARD,
    pause: Duration = Duration.ofMillis(5),
    interruptus: () -> Boolean = { false }
) {
    val steps = (stepsPerRotation * degrees / 360f).toInt()
    for (i in 1..steps) {
        if (interruptus()) break
        step(direction)
        SleepUtil.busySleep(pause.toNanos())
    }
}
