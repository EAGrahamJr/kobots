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

import com.diozero.api.DigitalOutputDevice
import java.lang.Long.max
import java.lang.Math.abs
import java.lang.Thread.sleep
import java.time.Duration

/**
 * Opinionated stepper-motor driver - derived from the [Freenove](https://freenove.com) kits/lessons for the [diozero](https://github.com/mattjlewis/diozero)
 * library.
 *
 * [Data sheet](https://www.ti.com/lit/gpn/uln2003a)
 *
 *  Note that each step requires a "wait period" before sending the next pulse, otherwise the speed limit of the motor can be exceeded. Rhe default is 3 ms.
 *
 * TODO allow for more construction params
 */
class ULN2003Driver(pins: Array<Int>) {
    private val counterClockwise = arrayOf(8, 4, 2, 1)
    private val clockwise = arrayOf(1, 2, 4, 8)
    private val pinOuts = if (pins.size != 4) throw IllegalArgumentException("Must specify 4 pins for ABCD") else pins.map { DigitalOutputDevice(it) }.toTypedArray()
    private val defaultDuration = Duration.ofMillis(3)

    /**
     * Move clockwise one step.
     */
    fun stepClockwise(duration: Duration = defaultDuration) {
        for (j in 0..3) {
            pinOuts.forEachIndexed { i, it ->
                if (clockwise[j] == (1 shl i)) it.on() else it.off()
            }
            pause(duration)
        }
    }

    /**
     * Move counter-clockwise one step in this time period. Note that this must then _wait_ for a bit before the next pulse can be sent (default 3 ms).
     */
    fun stepCounterClockwise(duration: Duration = defaultDuration) {
        for (j in 0..3) {
            pinOuts.forEachIndexed { i, it ->
                if (counterClockwise[j] == (1 shl i)) it.on() else it.off()
            }
            pause(duration)
        }
    }

    /**
     * Minimum 3ms pause between steps.
     */
    private fun pause(duration: Duration) {
        sleep(max(duration.toMillis(), defaultDuration.toMillis()))
    }

    /**
     * Stops the motor from moving.
     */
    fun stop() {
        pinOuts.forEach { it.off() }
    }

    /**
     * Rotate a certain number of degrees: positive is clockwise, negative is counter-clockwise.
     * Pauses between each "step" for the specified duration (default 3 ms).
     */
    fun rotate(degrees: Int, duration: Duration = Duration.ofMillis(3)) {
        val whichWay = if (degrees < 0) this::stepCounterClockwise else this::stepClockwise
        val steps = calculateSteps(degrees)

        if (steps > 0) (1..steps).forEach { whichWay(duration) }
    }

    companion object {
        private const val CIRCLE_STEPS = 512
        fun calculateSteps(degrees: Int) = (abs(degrees) * CIRCLE_STEPS) / 360
    }
}
