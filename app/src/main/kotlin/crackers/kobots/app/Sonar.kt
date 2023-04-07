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

package crackers.kobots.app

import com.diozero.devices.HCSR04
import crackers.kobots.devices.at
import kotlin.math.roundToInt

/**
 * Sweeps a servo through a defined angle and increments. Grabs 3 readings at each "stop" for an average.
 */
object Sonar : AutoCloseable {
    private const val TRIGGER_PIN = 23 // currently orange
    private const val ECHO_PIN = 22 // currently yellow
    private const val SERVO_PORT = 4

    private val ultrasound by lazy { HCSR04(TRIGGER_PIN, ECHO_PIN) }
    internal val servo by lazy { crickitHat.servo(SERVO_PORT) }

    private var current = 90f
    private val MIN_ANGLE = 45f
    private val MAX_ANGLE = 135f
    private val offset = 2
    private var direction = false

    init {
        servo at current
    }

    /**
     * Take the average of 3 readings (ignoring shit out of bounds).
     */
    fun ping(): Float = with(ultrasound) {
        var c = 0
        var sum = 0f
        (1..3).forEach {
            val v = distanceCm
            if (v < 100) {
                sum += v
                c++
            }
        }
        return if (c == 0) 100f else sum / c
    }

    fun next() {
        if (direction) {
            if ((current + offset) > MAX_ANGLE) direction = false
        } else {
            if ((current - offset) < MIN_ANGLE) direction = true
        }

        if (direction) current += offset else current -= offset
        servo at current
    }

    /**
     * Angle and distance
     */
    fun reading(): Reading {
        next()
        return Reading(current.roundToInt(), ping())
    }

    /**
     * Move back to zero position
     */
    fun zero() {
        servo at 90f
    }

    override fun close() {
        zero()
        ultrasound.close()
    }

    class Reading(val bearing: Int, val range: Float)
}
