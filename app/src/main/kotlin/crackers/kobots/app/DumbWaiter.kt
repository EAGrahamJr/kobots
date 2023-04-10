/*
 * Copyright 2022-2023 by E. A. Graham, Jr.
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

import com.diozero.api.DigitalInputDevice
import com.diozero.devices.sandpit.motor.BasicStepperMotor
import com.diozero.devices.sandpit.motor.StepperMotorInterface
import com.diozero.util.SleepUtil
import crackers.kobots.app.StatusFlags.flagFlag
import crackers.kobots.app.StatusFlags.flagPosition
import crackers.kobots.devices.expander.CRICKITHatDeviceFactory
import crackers.kobots.utilities.PURPLE
import java.awt.Color
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Global state machine.
 */
object StatusFlags {
    val runFlag = AtomicBoolean(true)
    val scanningFlag = AtomicBoolean(false)
    val flagFlag = AtomicBoolean(false)
    val collisionDetected = AtomicBoolean(false)

    val sonarScan = AtomicReference<Sonar.Reading>()
    val flagPosition = AtomicInteger(0)
}

const val WAIT_LOOP = 50L
const val RELAX_TIME = 30

// steps divided by actual ratio - 100 steps/ 1.666 ratio
const val GEAR_RATIO = 60

val crickitHat by lazy { CRICKITHatDeviceFactory() }
val flagStepper by lazy { BasicStepperMotor(200, crickitHat.motorStepperPort()) }
val neoPixel by lazy {
    crickitHat.neoPixel(30).apply {
        brightness = 0.1f
        autoWrite = true
    }
}

/**
 * Run this.
 */
fun main() {
    crickitHat.use { hat ->
        val runCheck = hat.touchDigitalIn(4).let { did ->
            {
                with(StatusFlags.runFlag) {
                    if (get() && did.value) set(false)
                    get()
                }
            }
        }
        val timeCheck = hat.touchDigitalIn(1).let { did ->
            {
                val now = Instant.now()
                if (!Screen.isOn() && did.value) {
                    Screen.startTimeAndTempThing(now)
                } else {
                    Screen.displayCurrentStatus(now)
                }
            }
        }

        val scanCheck = scanCheck(hat.touchDigitalIn(2))

        val flagChecker = hat.touchDigitalIn(3).let { did ->
            {
                if (did.value) flagFlag.compareAndSet(false, true)
                flagFlag.get()
            }
        }

        while (runCheck()) {
            val loopStartedAt = System.currentTimeMillis()
            neoCheck()
            if (flagChecker()) flagRunner()

            timeCheck()
            if (scanCheck()) {
                Sonar.reading().also { r ->
                    StatusFlags.sonarScan.set(r)
                    // if it's been re-set, re-check
                    if (!StatusFlags.collisionDetected.get()) StatusFlags.collisionDetected.set(r.range <= 30f)
                }
            }

            // adjust this dynamically?
            val runTime = System.currentTimeMillis() - loopStartedAt
            val waitFor = if (runTime > WAIT_LOOP) {
                0L
            } else {
                WAIT_LOOP - runTime
            }

            SleepUtil.busySleep(Duration.ofMillis(waitFor).toNanos())
        }

        Screen.close()
        Sonar.close()
        flagStepper.release()
    }
}

/**
 * Basically runs the scan for a set period of time before terminating.
 */
private var lastRotate = Instant.EPOCH
private fun scanCheck(did: DigitalInputDevice): () -> Boolean = {
    // if currently scanning, check to see if we stop (button state is immaterial)
    with(StatusFlags.scanningFlag) {
        val rotateCheck = Instant.now()

        if (get()) {
            // time to stop
            if (Duration.between(lastRotate, rotateCheck).toSeconds() > RELAX_TIME) {
                set(false)
                // auto clear collisions and re-set the servo
                StatusFlags.collisionDetected.set(false)
                Sonar.zero()
            }
            // otherwise keep going
        } else if (did.value) {
            lastRotate = rotateCheck
            set(true)
        }
        get()
    }
}

/**
 * Manage the color of the NeoPixel strip
 */
private var lastCheck = Color.BLUE
private val neoCheck = {
    val time = LocalTime.now()
    val stripColor = when {
        time.hour >= 23 && time.minute >= 30 -> Color.BLACK
        time.hour >= 21 -> Color.RED
        time.hour >= 8 -> PURPLE
        else -> Color.BLACK
    }
    if (stripColor != lastCheck) {
        neoPixel.fill(stripColor)
        lastCheck = stripColor
    }
}

private var flagDirection = false
private val flagRunner: () -> Unit = {
    // start
    if (flagPosition.get() == 0 || flagDirection) {
        flagStepper.step(StepperMotorInterface.Direction.BACKWARD)
        flagDirection = flagPosition.incrementAndGet() < GEAR_RATIO
    } else {
        flagStepper.step(StepperMotorInterface.Direction.FORWARD)
        flagFlag.set(flagPosition.decrementAndGet() > 0)
        // if this "transitions" to false, release the stepper
        if (!flagFlag.get()) flagStepper.release()
    }
}
