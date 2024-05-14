/*
 * Copyright 2022-2024 by E. A. Graham, Jr.
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

package crackers.kobots.app.enviro

import crackers.kobots.app.Jimmy.thermoStepper
import crackers.kobots.parts.app.KobotSleep
import org.tinylog.Logger
import kotlin.math.roundToInt

/**
 * This is very silly - it's a stepper motor that rotates to reflect the temperature in the room. Each degree of
 * temperature is 18 degrees of stepper rotation, with the median temperature being 75 degrees.
 */
object VeryDumbThermometer {

    private val degreesRange = (75f..90f)
    private val degreesTotal = degreesRange.endInclusive - degreesRange.start
    private val arcRange = (0..180)

    private var okayImHot = false

    fun reset() = setTemperature(degreesRange.start)

    /**
     * Allow for setting from external events.
     */
    fun setTemperature(temp: Float) {
        if (DieAufseherin.currentMode != DieAufseherin.SystemMode.MANUAL) {
            Logger.debug("Setting temp: {}", temp)

//            val tempOffset = (if (temp > degreesRange.endInclusive) degreesRange.endInclusive
//                        else if (temp < degreesRange.start) degreesRange.start
//                        else temp) - degreesRange.start
            val diff = (temp - degreesRange.start) / degreesTotal
            val angle = (180 * diff).roundToInt()
            while (!thermoStepper.rotateTo(angle)) {
                KobotSleep.millis(10)
            }
            thermoStepper.release()

//        okayImHot = if (temp > 80f) {
//            if (!okayImHot) TheArm.request(excuseMe)
//            true
//        } else {
//            false
//        }
        }
    }
}
