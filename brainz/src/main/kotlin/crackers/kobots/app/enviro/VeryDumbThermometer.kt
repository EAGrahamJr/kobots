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

import com.diozero.devices.sandpit.motor.BasicStepperController.StepStyle
import com.diozero.devices.sandpit.motor.BasicStepperMotor
import crackers.kobots.app.AppCommon
import crackers.kobots.parts.app.KobotSleep
import crackers.kobots.parts.movement.BasicStepperRotator
import crackers.kobots.parts.movement.sequence
import org.tinylog.Logger
import kotlin.math.roundToInt

/**
 * This is very silly - it's a stepper motor that rotates to reflect the temperature in the room. Each degree of
 * temperature is 18 degrees of stepper rotation, with the median temperature being 75 degrees.
 */
object VeryDumbThermometer : AppCommon.Startable {

    private val degreesRange = (70f..90f)
    private val degreesTotal = degreesRange.endInclusive - degreesRange.start
    private val arcRange = (0..180)

    private lateinit var thermoStepper: BasicStepperRotator

    fun init(stepper: BasicStepperMotor) {
        thermoStepper = BasicStepperRotator(stepper, stepStyle = StepStyle.INTERLEAVE, stepsPerRotation = 4096)
    }

    override fun start() {
        AppCommon.mqttClient.subscribeJSON("kobots_auto/office_enviro_temperature/state") { payload ->
            if (payload.has("state")) {
                val temp = payload.optString("state", "75").toFloat()
                setTemperature(temp)
            }
        }

        // assume we can get to temperature at this point
        with(AppCommon.hasskClient) {
            val payload = sensor("office_enviro_temperature").state().state
            val temp = payload.toFloat()
            setTemperature(temp)
        }
    }

    override fun stop() {
//        TODO("Not yet implemented")
    }

    val reset by lazy {
        sequence {
        action {
            thermoStepper rotate 0
        }
        action {
            execute {
                thermoStepper.release()
                true
            }
        }

    }
    }

    private fun setTemperature(temp: Float) {
        Logger.info("Setting temp: {}", temp)
        val diff = (temp - degreesRange.start) / degreesTotal
        val angle = (180 * diff).roundToInt()
        while (!thermoStepper.rotateTo(angle)) {
            KobotSleep.millis(10)
        }
        thermoStepper.release()
    }
}
