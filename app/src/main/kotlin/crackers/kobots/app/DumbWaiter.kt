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
import crackers.kobots.devices.expander.CRICKITHatDeviceFactory
import java.lang.Thread.sleep
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

object StatusFlags {
    val runFlag = AtomicBoolean(true)
    val scanningFlag = AtomicBoolean(false)
    val collisionDetected = AtomicBoolean(false)

    val sonarScan = AtomicReference<Sonar.Reading>()
}

val WAIT_LOOP = Duration.ofMillis(15).toNanos()
val RELAX_TIME = 30

val crickitHat by lazy { CRICKITHatDeviceFactory() }
val shoulderStepper by lazy { BasicStepperMotor(200, crickitHat.motorStepperPort()) }

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

        val calibrateTouch = hat.touchDigitalIn(3)
        var calibrate = -1f

        while (runCheck()) {
            timeCheck()
            if (scanCheck()) {
                Sonar.reading().also { r ->
                    StatusFlags.sonarScan.set(r)
                    // if it's been re-set, re-check
                    if (!StatusFlags.collisionDetected.get()) StatusFlags.collisionDetected.set(r.range <= 30f)
                }
            }

            // TODO do something with the stepper and make more shite here
//            if (StatusFlags.collisionDetected.get()) {
//                avoidCollision()
//            }

            // adjust this
            SleepUtil.busySleep(WAIT_LOOP)
        }
        Screen.close()
        Sonar.close()
        shoulderStepper.release()
    }
}

// TODO temporary
var collisionCounter = 0
private fun avoidCollision() {
    // T36->T20 -- T8-->24 (turntable inside)
    // ratio  == .6
    // forward drives CCW from the top
    (1..200).forEach {
        shoulderStepper.step(StepperMotorInterface.Direction.FORWARD)
        sleep(1)
    }
    collisionCounter++

    // if we've "avoided the collision", reset the detection
    // N.B. current rig has a CCW motion on the top gear
    // TODO this should be another "check" if doing rolling stuff
    if (collisionCounter >= 2) {
        collisionCounter = 0
        StatusFlags.collisionDetected.set(false)
    }
}

/**
 * Basically runs the scan for a set period of time before terminating.
 */
private fun scanCheck(did: DigitalInputDevice): () -> Boolean {
    var lastRotate = Instant.EPOCH

    return {
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
}
