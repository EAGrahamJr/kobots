/*
 * Copyright 2022-2025 by E. A. Graham, Jr.
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
import crackers.kobots.app.enviro.Sensei
import crackers.kobots.app.enviro.VeryDumbThermometer
import crackers.kobots.devices.expander.CRICKITHat
import crackers.kobots.devices.lighting.PixelBuf
import crackers.kobots.devices.lighting.WS2811
import crackers.kobots.parts.colorIntervalFromHSB
import crackers.kobots.parts.movement.BasicStepperRotator
import crackers.kobots.parts.movement.SequenceExecutor
import crackers.kobots.parts.movement.SequenceRequest
import crackers.kobots.parts.movement.ServoRotator
import crackers.kobots.parts.sleep
import java.awt.Color
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import crackers.kobots.app.enviro.DieAufseherin as DA

/**
 * Stuff for the CRICKIT
 */
object Jimmy : AppCommon.Startable, SequenceExecutor("brainz", AppCommon.mqttClient) {

    private lateinit var crickit: CRICKITHat

    val crickitNeoPixel by lazy { crickit.neoPixel(8).apply { brightness = .005f } }
    val statusPixel by lazy { crickit.statusPixel() }

    val switch1 by lazy {
        crickit.signalDigitalIn(1)
    }

    private val driveStepper by lazy { BasicStepperMotor(2048, crickit.unipolarStepperPort()) }
    private val motorStepper by lazy { BasicStepperMotor(2048, crickit.motorStepperPort()) }

    private val servo1 by lazy { crickit.servo(1, ServoTrim.MG90S).apply { angle = 0f } }
    private val servo2 by lazy { crickit.servo(2, ServoTrim.TOWERPRO_SG90).apply { angle = 0f } }
    private val servo3 by lazy { crickit.servo(3, ServoTrim.MG90S).apply { angle = 0f } }

    const val ABSOLUTE_AZIMUTH_LIMIT = 270
    val sunAzimuth by lazy {
        object : BasicStepperRotator(driveStepper, stepStyle = StepStyle.INTERLEAVE, stepsPerRotation = 4096) {
//            override fun rotateTo(angle: Int): Boolean {
//                return angleLocation > ABSOLUTE_AZIMUTH_LIMIT || Sensei.distance < 10  || super.rotateTo(angle)
//            }
            override fun reset() {
    val maxReset = driveStepper.stepsPerRotation / 4
                // run it forward a little bit
                logger.info("Stepping away")
                (1..driveStepper.stepsPerRotation / 8).forEach {
                    driveStepper.step(forwardDirection, StepStyle.INTERLEAVE)
                    5.milliseconds.sleep()
                }
    logger.info("Looking for home")
                var stepCounter = 0
    while (Sensei.distance > 10 && ++stepCounter < maxReset) {
                    driveStepper.step(backwardDirection, StepStyle.INTERLEAVE)
                    5.milliseconds.sleep()
                }
    if (Sensei.distance in (8..12)) logger.info("Home sweet home")
                else logger.error("Calibration too far out of bounds -- manual reset required")
                super.reset()
            }
        }
    }

    val sunElevation by lazy {
        ServoRotator(servo1, 0..90)
    }

    val wavyThing by lazy { ServoRotator(servo2, 0..90) }
//    val twistyThing by lazy { ServoRotator(servo3, 0..ServoTrim.MG90S.maxAngle) }

    // "backwards" from adafruit drives the piston _out_ to lower the lift
//    private const val HOW_TALL = 1024 * 14
//    val liftyThing by lazy { StepperLinearActuator(motorStepper, HOW_TALL, true, StepStyle.INTERLEAVE) }

    override fun canRun() = AppCommon.applicationRunning

    override fun start() {
        crickit = CRICKITHat()
        statusPixel.brightness = .1f
        VeryDumbThermometer.init(motorStepper, switch1)
    }

    private lateinit var stopLatch: CountDownLatch

    private const val SHUTDOWN_TIMEOUT = 90L

    override fun stop() {
        // already did this
        ::stopLatch.isInitialized && return

        // forces everything to stop
        super.stop()

        stopLatch = CountDownLatch(1)

        if (::crickit.isInitialized) {
            crickitNeoPixel.fill(WS2811.PixelColor(Color.RED, brightness = .1f))
            handleRequest(SequenceRequest(CannedSequences.home))
            if (!stopLatch.await(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS)) {
                logger.error("Arm not homed in $SHUTDOWN_TIMEOUT seconds")
            }
            // ensure the steppers are released
            sunAzimuth.release()
            crickitNeoPixel.fill(Color.BLACK)
            crickit.close()
        }
    }

    override fun preExecution() {
        DA.currentMode = DA.SystemMode.IN_MOTION
    }

    override fun postExecution() {
        driveStepper.release()
        motorStepper.release()
        DA.currentMode = DA.SystemMode.IDLE
        HAStuff.updateEverything()
        statusPixel.brightness = .1f
        if (::stopLatch.isInitialized) stopLatch.countDown()
    }

    fun cycleNeo(b: PixelBuf) {
        val allTheColors = colorIntervalFromHSB(0f, 359f, 360).map { WS2811.PixelColor(it, brightness = 0.005f) }
        var lastColor = 0
        while (true) {
            b.fill(allTheColors[lastColor++])
            if (lastColor >= allTheColors.size) lastColor = 0
            3.milliseconds.sleep()
        }
    }

    fun pulseNeo(buf: PixelBuf) {
        var lastBright = 0
        var diff = 1
        while (true) {
            val b = lastBright * .001f
            buf.fill(WS2811.PixelColor(Color.RED, brightness = b))
            if (lastBright == 10) {
                diff = -1
            } else if (lastBright == 0) {
                diff = 1
            }
            lastBright += diff
            5.milliseconds.sleep()
        }
    }
}
