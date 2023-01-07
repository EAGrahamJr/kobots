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
import com.diozero.util.SleepUtil
import java.lang.Long.max
import java.lang.Math.abs
import java.time.Duration

/**
 * Opinionated, simple stepper-motor driver - derived from the [Freenove](https://github.com/Freenove) kits for the
 * [diozero](https://github.com/mattjlewis/diozero) library.
 *
 * Original code: https://github.com/Freenove/Freenove_Ultimate_Starter_Kit_for_Raspberry_Pi/tree/master/Code/Python_Code/16.1.1_SteppingMotor
 *
 * Note that each step requires a "wait period" before sending the next pulse, otherwise the speed limit of the motor can be exceeded. Rhe default is 3 ms.
 *
 * TODO allow for more construction params
 *
 * TODO translate the delay into RPM
 * - for example 512 steps with 3ms pause between is 1 rotation every 1.536 seconds
 * - roughly 39 RPM
 */
class ULN2003Driver(pins: Array<Int>) {
    private val counterClockwise = arrayOf(8, 4, 2, 1)
    private val clockwise = arrayOf(1, 2, 4, 8)
    private val pinOuts = if (pins.size != 4) throw IllegalArgumentException("Must specify 4 pins for ABCD") else pins.map { DigitalOutputDevice(it) }.toTypedArray()

    /**
     * Move clockwise one step.
     */
    fun stepClockwise(duration: Duration = DEFAULT_PAUSE) {
        step(clockwise, duration)
    }

    /**
     * Move counter-clockwise one step.
     */
    fun stepCounterClockwise(duration: Duration = DEFAULT_PAUSE) {
        step(clockwise.reversedArray(), duration)
    }

    private fun step(hilo: Array<Int>, duration: Duration) {
        for (j in 0..3) {
            pinOuts.forEachIndexed { i, it ->
                if (hilo[j] == (1 shl i)) it.on() else it.off()
            }
            SleepUtil.sleepMillis(max(duration.toMillis(), DEFAULT_PAUSE.toMillis()))
        }
    }

    /**
     * Stops the motor from moving.
     */
    fun stop() {
        pinOuts.forEach { it.off() }
    }

    /**
     * Rotate a certain number of degrees: positive is clockwise, negative is counter-clockwise.
     */
    fun rotate(degrees: Int, duration: Duration = DEFAULT_PAUSE) {
        val whichWay = if (degrees < 0) this::stepCounterClockwise else this::stepClockwise
        val steps = calculateSteps(degrees)

        if (steps > 0) (1..steps).forEach { _ -> whichWay(duration) }
    }

    companion object {
        val DEFAULT_PAUSE = Duration.ofMillis(3)
        private const val CIRCLE_STEPS = 512
        fun calculateSteps(degrees: Int) = (abs(degrees) * CIRCLE_STEPS) / 360
    }
}
