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

import com.diozero.devices.sandpit.motor.BasicStepperMotor
import com.diozero.devices.sandpit.motor.StepperMotorInterface
import com.diozero.util.SleepUtil
import crackers.kobots.app.StatusFlags.stepperActivationFlag
import crackers.kobots.app.StatusFlags.stepperPosition
import crackers.kobots.devices.expander.CRICKITHatDeviceFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Global state machine.
 */
object StatusFlags {
    val runFlag = AtomicBoolean(true)
    val proximityServoActive = AtomicBoolean(false)
    val proximityModeSelectionActive = AtomicBoolean(false)

    val stepperActivationFlag = AtomicBoolean(false)
    val stepperPosition = AtomicInteger(0)
    val proximityReading = AtomicInteger(100)
    val colorMode = AtomicReference(Stripper.NeoPixelControls.DEFAULT)
}

// devices
val crickitHat by lazy { CRICKITHatDeviceFactory() }
val nemaStepper by lazy { BasicStepperMotor(200, crickitHat.motorStepperPort()) }

/**
 * "Global" flag for loop/app termination
 */
val runCheck by lazy {
    crickitHat.touchDigitalIn(4).let { did ->
        {
            with(StatusFlags.runFlag) {
                if (get() && did.value) set(false)
                get()
            }
        }
    }
}

const val WAIT_LOOP = 50L

/**
 * Run this.
 */
fun main() {
    crickitHat.use { hat ->
        val screenCheck = crickitHat.touchDigitalIn(1).let { did ->
            {
                if (!Screen.isOn() && did.value) Screen.startTimeAndTempThing()
                Screen.displayCurrentStatus()
            }
        }

        val flagChecker = hat.touchDigitalIn(3).let { did ->
            {
                with(stepperActivationFlag) {
                    if (did.value) compareAndSet(false, true)
                    get()
                }
            }
        }

        val controllerCheck = hat.touchDigitalIn(2).let { did ->
            {
                if (did.value) ControlThing.activateSensor()
                ControlThing.execute()
            }
        }

        // main loop!!!!!
        while (runCheck()) {
            val loopStartedAt = System.currentTimeMillis()

            if (flagChecker()) flagRunner()
            screenCheck()

            controllerCheck()

            // do these last because they take the most time?
            Stripper.execute()

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
        ControlThing.close()
        nemaStepper.release()
    }
}

// TODO temporary stepper function
private var flagDirection = false
private val MAX_STEPS = 129
private val flagRunner: () -> Unit = {
    // start
    if (stepperPosition.get() == 0 || flagDirection) {
        nemaStepper.step(StepperMotorInterface.Direction.BACKWARD)
        flagDirection = stepperPosition.incrementAndGet() < MAX_STEPS
    } else {
        nemaStepper.step(StepperMotorInterface.Direction.FORWARD)
        stepperActivationFlag.set(stepperPosition.decrementAndGet() > 0)
        // if this "transitions" to false, release the stepper
        if (!stepperActivationFlag.get()) nemaStepper.release()
    }
}
