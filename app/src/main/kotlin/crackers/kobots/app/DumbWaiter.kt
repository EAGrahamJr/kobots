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
import crackers.kobots.devices.at
import crackers.kobots.devices.expander.CRICKITHatDeviceFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

object StatusFlags {
    val runFlag = AtomicBoolean(true)
    val scanningFlag = AtomicBoolean(false)
    val collisionDetected = AtomicBoolean(false)

    val sonarScan = AtomicReference<Sonar.Reading>()

    /**
     * One time setter: flips based on initial condition when [did] is activated
     */
    fun flagSetter(did: DigitalInputDevice, flag: AtomicBoolean): () -> Boolean {
        val initial = flag.get()
        return {
            with(flag) {
                if (get() == initial && did.value) set(!initial)
                get()
            }
        }
    }
}

val WAIT_LOOP = Duration.ofMillis(15).toNanos()
val RELAX_TIME = 15

val crickitHat by lazy { CRICKITHatDeviceFactory() }
val shoulderStepper by lazy { BasicStepperMotor(200, crickitHat.motorStepperPort()) }

fun main() {
    crickitHat.use { hat ->
        val runCheck = StatusFlags.flagSetter(hat.touchDigitalIn(4), StatusFlags.runFlag)
        val timeCheck = hat.touchDigitalIn(1).let { did ->
            {
                val now = Instant.now()
                if (!Screen.isOn() && did.value) {
                    Screen.startTimeAndTempThing(now)
                } else {
                    Screen.displayCurrentStatus(now)
                }
                now
            }
        }

        val scanCheck = scanCheck(hat.touchDigitalIn(2))
        var collisionCounter = 0

        val calibrateTouch = hat.touchDigitalIn(3)
        var calibrate = -1f

        while (runCheck()) {
            val nowTime = timeCheck()
            if (scanCheck()) {
                Sonar.reading().also { r ->
                    StatusFlags.sonarScan.set(r)
                    // if it's been re-set, re-check
                    if (!StatusFlags.collisionDetected.get()) StatusFlags.collisionDetected.set(r.range <= 30f)
                }
            }

            // TODO do something with the stepper and make more shite here
            if (StatusFlags.collisionDetected.get()) {
                // just do a quick 200-step rotation for now
                (1..20).forEach {
                    shoulderStepper.step(StepperMotorInterface.Direction.FORWARD)
                    SleepUtil.busySleep(Duration.ofMillis(5).toNanos())
                }
                collisionCounter++
                // if we've "avoided the collision", reset the detection
                if (collisionCounter >= 2) {
                    collisionCounter = 0
                    StatusFlags.collisionDetected.set(false)
                }
            }

            if (!StatusFlags.scanningFlag.get() && !StatusFlags.collisionDetected.get() && calibrateTouch.value) {
                calibrate = when {
                    calibrate == 0f -> 90f
                    calibrate == 90f -> 180f
                    else -> 0f
                }
                Sonar.servo at calibrate
                SleepUtil.busySleep(Duration.ofMillis(50).toNanos()) // finger off the button
            }

            // adjust this
            SleepUtil.busySleep(WAIT_LOOP)
        }
        Screen.close()
        Sonar.close()
    }
}

private fun scanCheck(did: DigitalInputDevice): () -> Boolean {
    var lastRotate = Instant.EPOCH

    return {
        val rotateCheck = Instant.now()
        // if currently scanning, check to see if we stop (button state is immaterial)
        if (StatusFlags.scanningFlag.get()) {
            // time to stop
            if (Duration.between(lastRotate, rotateCheck).toSeconds() > RELAX_TIME) {
                StatusFlags.scanningFlag.set(false)
                StatusFlags.collisionDetected.set(false)
                Sonar.zero()
            }
            // otherwise keep going
        } else if (did.value) {
            lastRotate = rotateCheck
            StatusFlags.scanningFlag.set(true)
        }
        StatusFlags.scanningFlag.get()
    }
}
