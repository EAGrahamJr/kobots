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

import com.diozero.devices.sandpit.motor.BasicStepperController.*
import com.diozero.devices.sandpit.motor.BasicStepperMotor
import crackers.kobots.app.otherstuff.Jeep
import crackers.kobots.parts.app.KobotSleep
import crackers.kobots.parts.movement.BasicStepperRotator
import crackers.kobots.parts.movement.sequence
import org.tinylog.Logger
import kotlin.math.roundToInt

/**
 * This is very silly - it's a stepper motor that rotates to reflect the temperature in the room. Each degree of
 * temperature is 18 degrees of stepper rotation, with the median temperature being 75 degrees.
 */
object VeryDumbThermometer {

    private val degreesRange = (70f..90f)
    private val degreesTotal = degreesRange.endInclusive - degreesRange.start
    private val arcRange = (0..180)

    private val motor by lazy {
        val pins = Jeep.stepper1Pins.map { GpioStepperPin(it) }.toTypedArray()
        val controller = UnipolarBasicController(pins)
        BasicStepperMotor(2048, controller)
    }

    private val thermoStepper by lazy {
        BasicStepperRotator(motor, stepStyle = StepStyle.INTERLEAVE, stepsPerRotation = 4096)
    }

    val reset = sequence {
        action {
            thermoStepper rotate 0
        }
    }

    /**
     * Allow for setting from external events.
     */
    fun setTemperature(temp: Float) {
        Logger.info("Setting temp: {}", temp)
        val diff = (temp - degreesRange.start) / degreesTotal
        val angle = (180 * diff).roundToInt()
        while (!thermoStepper.rotateTo(angle)) {
            KobotSleep.millis(10)
        }
        thermoStepper.release()
    }
}
