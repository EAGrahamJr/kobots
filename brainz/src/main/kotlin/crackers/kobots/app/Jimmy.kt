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

import com.diozero.api.ServoTrim
import com.diozero.devices.sandpit.motor.BasicStepperController.StepStyle
import com.diozero.devices.sandpit.motor.BasicStepperMotor
import crackers.kobots.app.enviro.HAStuff
import crackers.kobots.devices.expander.CRICKITHat
import crackers.kobots.devices.lighting.WS2811
import crackers.kobots.devices.sensors.VCNL4040
import crackers.kobots.parts.app.KobotSleep
import crackers.kobots.parts.movement.BasicStepperRotator
import crackers.kobots.parts.movement.SequenceExecutor
import crackers.kobots.parts.movement.SequenceRequest
import crackers.kobots.parts.movement.ServoRotator
import java.awt.Color
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import crackers.kobots.app.enviro.DieAufseherin as DA

/**
 * Stuff for the CRICKIT
 */
object Jimmy : AppCommon.Startable, SequenceExecutor("brainz", AppCommon.mqttClient) {
    const val CRACKER = 90

    private lateinit var crickit: CRICKITHat
    private lateinit var polly: VCNL4040

    private fun VCNL4040.wantsCracker() = proximity < CRACKER
    private fun VCNL4040.hasCracker() = !wantsCracker()

    val crickitNeoPixel by lazy { crickit.neoPixel(8).apply { brightness = .005f } }

    const val ABSOLUTE_AZIMUTH_LIMIT = 270
    val sunAzimuth by lazy {
        val azimuthStepper by lazy { BasicStepperMotor(4096, crickit.unipolarStepperPort()) }
        object : BasicStepperRotator(azimuthStepper, stepStyle = StepStyle.INTERLEAVE) {
            override fun limitCheck(whereTo: Int): Boolean {
                return angleLocation > ABSOLUTE_AZIMUTH_LIMIT || polly.hasCracker().also {
                    if (it) super.reset()
                }
            }

            override fun reset() {
                // run it forward a little bit
                logger.info("Rotate forward")
                (1..azimuthStepper.stepsPerRotation / 8).forEach {
                    azimuthStepper.step(forwardDirection)
                    KobotSleep.millis(10)
                }
                logger.info("Looking for cracker")
                while (polly.wantsCracker()) {
                    azimuthStepper.step(backwardDirection)
                    KobotSleep.millis(10)
                }
                super.reset()
            }
        }
    }

    private val elevationServo by lazy { crickit.servo(1, ServoTrim.MG90S).apply { angle = 0f } }
    val sunElevation by lazy {
        object : ServoRotator(elevationServo, 0..90) {
            override fun rotateTo(angle: Int): Boolean {
                return super.rotateTo(if (angle > 0) angle else 0)
            }
        }
    }

    val wavyThing by lazy {
        ServoRotator(crickit.servo(2, ServoTrim.TOWERPRO_SG90).apply { angle = 0f }, 0..180)
    }

    override fun canRun() = AppCommon.applicationRunning

    override fun start() {
        polly = VCNL4040().apply {
            ambientLightEnabled = true
            proximityEnabled = true
            proximityLEDCurrent = VCNL4040.LEDCurrent.LED_100MA
        }
        crickit = CRICKITHat()
    }

    private lateinit var stopLatch: CountDownLatch

    override fun stop() {
        // already did this
        ::stopLatch.isInitialized && return

        // forces everything to stop
        super.stop()

        stopLatch = CountDownLatch(1)

        if (::crickit.isInitialized) {
            crickitNeoPixel.fill(WS2811.PixelColor(Color.RED, brightness = .1f))
            handleRequest(SequenceRequest(CannedSequences.home))
            if (!stopLatch.await(30, TimeUnit.SECONDS)) {
                logger.error("Arm not homed in 30 seconds")
            }
            // ensure the steppers are released
            sunAzimuth.release()
            crickitNeoPixel.fill(Color.BLACK)
            crickit.close()
        }
        if (::polly.isInitialized) polly.close()
    }

    override fun preExecution() {
        DA.currentMode = DA.SystemMode.IN_MOTION
    }

    override fun postExecution() {
        sunAzimuth.release()
        DA.currentMode = DA.SystemMode.IDLE
        HAStuff.updateEverything()
        if (::stopLatch.isInitialized) stopLatch.countDown()
    }
}
