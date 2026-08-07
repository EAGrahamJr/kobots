/*
 * Copyright 2022-2026 by E. A. Graham, Jr.
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
import com.diozero.devices.sandpit.motor.BasicStepperController
import com.diozero.devices.sandpit.motor.BasicStepperMotor
import crackers.kobots.app.enviro.HAStuff
import crackers.kobots.devices.expander.CRICKITHat
import crackers.kobots.devices.lighting.WS2811
import crackers.kobots.devices.sensors.VL6180X
import crackers.kobots.parts.GOLDENROD
import crackers.kobots.parts.PURPLE
import crackers.kobots.parts.movement.async.AsyncServoRotator
import crackers.kobots.parts.movement.async.EasingFunction
import crackers.kobots.parts.movement.async.EventBus.subscribe
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.awt.Color
import java.lang.System.err
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import crackers.kobots.app.enviro.DieAufseherin as DA


/**
 * Stuff for the CRICKIT
 */
object Jimmy : AppCommon.Startable {
    private const val NERMAL = .05f

    private lateinit var crickit: CRICKITHat
    private val stopLatch = AtomicBoolean(false)
    private val logger = LoggerFactory.getLogger("Jimmy")

    val neoPixel by lazy { crickit.neoPixel(16).apply { brightness = .005f } }

    private val driveStepper by lazy { BasicStepperMotor(1024, crickit.unipolarStepperPort()) }
//    private val motorStepper by lazy { BasicStepperMotor(2048, crickit.motorStepperPort()) }

    private val cheapServoTrim = ServoTrim(500, 2500, 190L)

    private val servo1 by lazy { crickit.servo(1, cheapServoTrim).apply { angle = 0f } }
    private val servo2 by lazy { crickit.servo(2, cheapServoTrim).apply { angle = 0f } }
    private val servo3 by lazy { crickit.servo(3, ServoTrim.MG90S).apply { angle = 0f } }
    private val servo4 by lazy { crickit.servo(4, ServoTrim.MG90S).apply { angle = 0f } }
    private val servoRange = ServoTrim.MG90S.minAngle..ServoTrim.MG90S.maxAngle

    val runLatch = Mutex()

    val rotor1 by lazy {
        val range = (0..45)
        object : AsyncServoRotator(servo1, range, range) {
            override suspend fun myLittleKillSwitch() = stopLatch.get()
        }
    }
    val rotor2 by lazy {
        val range = (0..125)
        object : AsyncServoRotator(servo2, range, range) {
            override suspend fun myLittleKillSwitch() = stopLatch.get()
        }
    }
    val rotor3 by lazy {
        object : AsyncServoRotator(servo3, servoRange, servoRange) {
            override suspend fun myLittleKillSwitch() = stopLatch.get()
        }
    }
    val rotor4 by lazy {
        object : AsyncServoRotator(servo4, servoRange, servoRange) {
            override suspend fun myLittleKillSwitch() = stopLatch.get()
        }
    }

    val driveStepperRotator by lazy {
        val toffle = VL6180X()
        val calibrationStop = {
            val range = toffle.range
            (range <= 15).also {
                if (it) logger.error("Range trigger ${range}")
            }
        }
        object : CalibratingRotator(
            driveStepper, calibrationStop,
            gearRatio = 2.5f, // the omg how frickin' big is this thing turntable 40-90
            stepStyle = BasicStepperController.StepStyle.DOUBLE,
        ) {
            override suspend fun myLittleKillSwitch() = stopLatch.get()
            override suspend fun rotateAsync(angle: Int, time: Duration, easing: EasingFunction) {
                runCatching {
                    super.rotateAsync(angle, time, easing)
                }.onFailure {
                    logger.error(it.localizedMessage)
                }
            }
        }
    }

//    val motorStepperRotator by lazy {
//        AsyncStepperRotator(motorStepper, stepPause = 1.milliseconds, gearRatio = 0.3003003f)
//    }

    override fun start() {
        crickit = CRICKITHat().apply {
            statusPixel().fill(Color.BLACK)
        }

//        reCalibrate()

        subscribe<CannedSequences.MoveMessage>(RUN_STUFF) { msg ->
            runLatch.withLock {
                preExecution()
                try {
                    msg.runThis()
                } catch (e: Exception) {
                    err.println("Error during movement execution: ${e.message}")
                }
                if (!stopLatch.get()) postExecution()
            }
        }
    }

    override fun stop() {
        // already did this
        if (!stopLatch.compareAndSet(false, true)) return

        if (::crickit.isInitialized) {
            // assume everyone is "home"
            runCatching {
                driveStepper.release()
                logger.info("Drive stepper released")
//                motorStepper.release()
                logger.info("Motor stepper released")
            }.onFailure {
                logger.error("Failed to release steppers", it)
            }
            runCatching {
                neoPixel.fill(Color.BLACK)
                crickit.statusPixel().fill(Color.BLACK)
                logger.info("Lights off")
            }.onFailure {
                logger.error("Failed to kill light", it)
            }

            logger.warn("Closing CRICKIT")
            crickit.close()
        }
    }

    fun preExecution() {
        if (DA.currentMode == DA.SystemMode.SHUTDOWN) {
            runCatching {
                neoPixel[0] = WS2811.PixelColor(PURPLE, brightness = NERMAL)
            }
        } else {
            DA.currentMode = DA.SystemMode.IN_MOTION
            runCatching {
                neoPixel[0] = WS2811.PixelColor(Color.GREEN, brightness = NERMAL)
            }
        }
    }

    fun postExecution() {
        driveStepper.release()
//        motorStepper.release()
        if (DA.currentMode != DA.SystemMode.SHUTDOWN) {
            DA.currentMode = DA.SystemMode.IDLE
            HAStuff.updateEverything()
        }

        runCatching {
            neoPixel[0] = WS2811.PixelColor(GOLDENROD, brightness = NERMAL)
        }
    }

    fun reCalibrate() = runBlocking {
        runLatch.withLock {
            driveStepper.release()
//            motorStepper.release()
            preExecution()
            driveStepperRotator.runCalibration()
            postExecution()
        }
    }
}
