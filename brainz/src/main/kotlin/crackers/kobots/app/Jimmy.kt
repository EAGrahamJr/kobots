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

package crackers.kobots.app

import com.diozero.devices.sandpit.motor.BasicStepperMotor
import com.diozero.devices.sandpit.motor.StepperMotorInterface
import crackers.kobots.app.enviro.HAStuff
import crackers.kobots.devices.expander.CRICKITHat
import crackers.kobots.devices.sensors.VCNL4040
import crackers.kobots.parts.app.KobotSleep
import crackers.kobots.parts.movement.BasicStepperRotator
import crackers.kobots.parts.movement.SequenceExecutor
import crackers.kobots.parts.movement.StepperLinearActuator
import java.awt.Color
import crackers.kobots.app.enviro.DieAufseherin as DA

/**
 * Stuff for the CRICKIT
 */
object Jimmy : AppCommon.Startable, SequenceExecutor("brainz", AppCommon.mqttClient) {
    const val CRACKER = 90
//

    private lateinit var crickit: CRICKITHat
    private lateinit var polly: VCNL4040

    private fun VCNL4040.wantsCracker() = proximity < CRACKER
    private fun VCNL4040.hasCracker() = !wantsCracker()

    val crickitNeoPixel by lazy { crickit.neoPixel(8).apply { brightness = .005f } }

    val thermoStepper by lazy {
        val stepper = BasicStepperMotor(200, crickit.motorStepperPort())
        BasicStepperRotator(stepper, gearRatio = 1f)
    }

    private val MAX_LIFT = 16_000
    private val liftStepper by lazy { BasicStepperMotor(1024, crickit.unipolarStepperPort()) }
    val lifter by lazy {
        object : StepperLinearActuator(liftStepper, MAX_LIFT, true) {
            override fun limitCheck(whereTo: Int): Boolean {
                return whereTo <= currentPercent && polly.hasCracker().also {
                    if (it) reset()
                }
            }
        }
    }

    override fun canRun() = AppCommon.applicationRunning

    override fun start() {
        polly = VCNL4040().apply {
            ambientLightEnabled = true
            proximityEnabled = true
        }
        crickit = CRICKITHat()
//        DisplayDos.text("Wait...")
        // it would be nice if we had a max stop, too?
        if (polly.hasCracker()) (1..MAX_LIFT / 4).forEach {
            liftStepper.step(StepperMotorInterface.Direction.BACKWARD)
            KobotSleep.millis(1)
        }
        while (polly.wantsCracker()) {
            liftStepper.step(StepperMotorInterface.Direction.FORWARD)
            KobotSleep.millis(1)
        }
        lifter.release()
//        DisplayDos.text("")
    }

    override fun stop() {
        super.stop()
        if (::crickit.isInitialized) {
            crickitNeoPixel.fill(Color.BLACK)
            // ensure the steppers are released
            thermoStepper.release()
            lifter.release()
            crickit.close()
        }
        if (::polly.isInitialized) polly.close()
    }

    override fun preExecution() {
        DA.currentMode = DA.SystemMode.IN_MOTION
    }

    override fun postExecution() {
        lifter.release()
        DA.currentMode = DA.SystemMode.IDLE
        HAStuff.updateEverything()
    }
}
