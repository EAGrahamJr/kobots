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

import com.diozero.api.DigitalInputDevice
import com.diozero.devices.sandpit.motor.BasicStepperController.StepStyle
import com.diozero.devices.sandpit.motor.BasicStepperMotor
import com.diozero.devices.sandpit.motor.StepperMotorInterface
import crackers.kobots.app.AppCommon
import crackers.kobots.parts.app.KobotSleep
import crackers.kobots.parts.movement.BasicStepperRotator
import crackers.kobots.parts.movement.action
import crackers.kobots.parts.movement.sequence
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

/**
 * This is very silly - it's a stepper motor that rotates to reflect the temperature in the room. Each degree of
 * temperature is 18 degrees of stepper rotation, with the median temperature being 75 degrees.
 */
object VeryDumbThermometer {
    private val logger = LoggerFactory.getLogger("DUmpTemp")
    private val degreesRange = (70f..90f)
    private val degreesTotal = degreesRange.endInclusive - degreesRange.start
    private val arcRange = (0..180)

    private lateinit var theStepper: BasicStepperMotor
    private lateinit var thermoStepper: BasicStepperRotator
    private lateinit var limitSwitch: DigitalInputDevice

    fun init(stepper: BasicStepperMotor, switch: DigitalInputDevice) {
        theStepper = stepper
        thermoStepper = BasicStepperRotator(stepper, stepStyle = StepStyle.INTERLEAVE, stepsPerRotation = 4096)
        limitSwitch = switch

        AppCommon.mqttClient.subscribeJSON("kobots_auto/office_enviro_temperature/state") { payload ->
            if (payload.has("state")) {
                val temp = payload.optString("state", "75").toFloat()
                setTemperature(temp)
            }
        }

        // assume we can get to temperature at this point
        getTemperatureNow()
    }

    private fun getTemperatureNow() = with(AppCommon.hasskClient) {
        val payload = sensor("office_enviro_temperature").state().state
        val temp = payload.toFloat()
        logger.info("Starting temp ${temp}")
        setTemperature(temp)
        true
    }

    val home by lazy {
        action { thermoStepper rotate 0 }
    }
    val reset by lazy {
        sequence {
            this append home
            action {
                execute {
                    // backwards
                    logger.info("Looking for switch")
                    while (!limitSwitch.value) {
                        theStepper.step(StepperMotorInterface.Direction.BACKWARD, StepStyle.INTERLEAVE)
                    }
                    // forwards
                    logger.info("Get off my yard!")
                    while (!limitSwitch.value) {
                        theStepper.step(StepperMotorInterface.Direction.FORWARD, StepStyle.INTERLEAVE)
                    }
                    logger.info("Wee bit more")
                    repeat(400) {
                        theStepper.step(StepperMotorInterface.Direction.FORWARD, StepStyle.INTERLEAVE)
                    }
                    thermoStepper.release()
                    thermoStepper.reset()
                    getTemperatureNow()
                }
            }

        }
    }

    private fun setTemperature(temp: Float) {
        logger.info("Setting temp: {}", temp)
        val diff = (temp - degreesRange.start) / degreesTotal
        val angle = (180 * diff).roundToInt()
        while (!thermoStepper.rotateTo(angle)) {
            KobotSleep.millis(10)
        }
        thermoStepper.release()
    }
}
