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
import com.diozero.devices.sandpit.motor.BasicStepperMotor
import crackers.kobots.app.CannedSequences.MoveMessage
import crackers.kobots.app.enviro.HAStuff
import crackers.kobots.devices.expander.CRICKITHat
import crackers.kobots.parts.GOLDENROD
import crackers.kobots.parts.movement.async.AsyncRotator.Companion.asyncServoRotator
import crackers.kobots.parts.movement.async.EventBus.subscribe
import java.awt.Color
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import crackers.kobots.app.enviro.DieAufseherin as DA


/**
 * Stuff for the CRICKIT
 */
object Jimmy : AppCommon.Startable {
    private const val NERMAL = .05f

    private lateinit var crickit: CRICKITHat
    private val stopLatch = AtomicBoolean(false)

    //    val crickitNeoPixel by lazy { crickit.neoPixel(8).apply { brightness = .005f } }
    val statusPixel by lazy { crickit.statusPixel() }

    private val driveStepper by lazy { BasicStepperMotor(2048, crickit.unipolarStepperPort()) }
    private val motorStepper by lazy { BasicStepperMotor(2048, crickit.motorStepperPort()) }

    private val servo1 by lazy { crickit.servo(1, ServoTrim.MG90S).apply { angle = 0f } }
    private val servo2 by lazy { crickit.servo(2, ServoTrim.MG90S).apply { angle = 0f } }
    private val servo3 by lazy { crickit.servo(3, ServoTrim.MG90S).apply { angle = 0f } }

    val rotorV by lazy {
        servo1.asyncServoRotator().apply {
            myLittleKillSwitch = { stopLatch.get() }
        }
    }
    val rotorH by lazy {
        servo3.asyncServoRotator().apply {
            myLittleKillSwitch = { stopLatch.get() }
        }
    }


    val runLatch = ReentrantLock()

    override fun start() {
        crickit = CRICKITHat()

        subscribe<MoveMessage>(RUN_STUFF) { msg ->
            runLatch.withLock {
                preExecution()
                msg.runThis()
                if (!stopLatch.get()) postExecution()
            }
        }
    }

    override fun stop() {
        // already did this
        if (!stopLatch.compareAndSet(false, true)) return

        if (::crickit.isInitialized) {
            runCatching {
                statusPixel.fill(Color.BLACK)
                statusPixel.brightness = NERMAL
            }
            // "home" everything
            runCatching { driveStepper.release() }
            runCatching { motorStepper.release() }

            // supposedly we can control the servos now
            // TODO this should be cleaner
            servo2.angle = 0f
            servo3.angle = 0f
            servo1.angle = 0f

            println("Closing CRICKIT")
            crickit.close()
        }
    }

    fun preExecution() {
        DA.currentMode = DA.SystemMode.IN_MOTION
        runCatching {
            statusPixel.brightness = .3f
            statusPixel.fill(Color.GREEN)
        }
    }

    fun postExecution() {
        driveStepper.release()
        motorStepper.release()
        DA.currentMode = DA.SystemMode.IDLE
        HAStuff.updateEverything()
        runCatching {
            statusPixel.brightness = NERMAL
            statusPixel.fill(GOLDENROD)
        }
    }
}
