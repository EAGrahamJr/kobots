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

import com.diozero.devices.sandpit.motor.BasicStepperMotor
import crackers.kobots.app.crickitHat
import crackers.kobots.parts.app.KobotSleep
import crackers.kobots.parts.movement.BasicStepperRotator
import org.tinylog.Logger

/**
 * This is very silly - it's a stepper motor that rotates to reflect the temperature in the room. Each degree of
 * temperature is 18 degrees of stepper rotation, with the median temperature being 75 degrees.
 */
object VeryDumbThermometer {
    val thermoStepper by lazy {
        val stepper = BasicStepperMotor(2048, crickitHat.unipolarStepperPort())
        BasicStepperRotator(stepper, gearRatio = 1.28f, reversed = true)
    }

    private const val DEGREES_TO_ANGLES = 18f // this comes out to 5 degree temp change == 90 degree stepper change
    private const val TEMP_OFFSET = 75f // median temperature

    private var okayImHot = false

    fun reset() = setTemperature(TEMP_OFFSET)

    /**
     * Allow for setting from external events.
     */
    fun setTemperature(temp: Float) {
        if (DieAufseherin.currentMode != DieAufseherin.SystemMode.MANUAL) {
            Logger.debug("Setting temp: {}", temp)

//        okayImHot = if (temp > 80f) {
//            if (!okayImHot) TheArm.request(excuseMe)
//            true
//        } else {
//            false
//        }
            val angle = ((temp - TEMP_OFFSET) * DEGREES_TO_ANGLES).toInt()
            while (!thermoStepper.rotateTo(angle)) {
                KobotSleep.millis(10)
            }
            thermoStepper.release()
        }
    }
}
