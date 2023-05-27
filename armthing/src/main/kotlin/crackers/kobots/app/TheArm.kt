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

import com.diozero.api.ServoDevice
import com.diozero.api.ServoTrim
import com.diozero.devices.sandpit.motor.BasicStepperMotor
import com.diozero.devices.sandpit.motor.StepperMotorInterface
import crackers.kobots.devices.at
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private class DesiredState(
    val finalState: TheArm.State, // angle
    val waistPosition: Int,      // steps
    val shoulderPosition: Int,
    val elbowPosition: Int
)

/**
 * First attempt at a controlled "arm-like" structure.
 */
object TheArm {
    const val STATE_TOPIC = "TheArm.State"
    enum class Request {
        NONE, REST, DEPLOY_FRONT, GUARD, CALIBRATE
    }

    enum class State {
        BUSY, REST, FRONT, GUARDING, CALIBRATION
    }

    private const val SHOULDER_DELTA = 1
    private const val SHOULDER_MIN = 0  // pulled in
    private const val SHOULDER_MAX = 90 // translates to about 60 degrees due to gear ratios

    private const val ELBOW_DELTA = 1
    private const val ELBOW_MIN = 90
    private const val ELBOW_MAX = 180

    private const val GRIPPER_OPEN = 180
    private const val GRIPPER_CLOSE = 130

    private const val MAX_WAIST_STEPS = 200 * 1.39 // 36->12=24->60
    private val QUARTER = (MAX_WAIST_STEPS / 4f).roundToInt()

    // shoulder: current configuration is 0 for UP and 90 for down
    // waist: start depends on manual adjustment, but tries to keep a notion of "where it is"
    private val desiredStates = mapOf(
        Request.DEPLOY_FRONT to (DesiredState(State.FRONT, QUARTER, SHOULDER_MAX, ELBOW_MAX)),
        Request.REST to DesiredState(State.REST, 0, SHOULDER_MIN, ELBOW_MIN)
    )

    // hardware! =====================================================================================================
    private val waistStepper by lazy { BasicStepperMotor(200, crickitHat.motorStepperPort()) }

    private val shoulderServo by lazy {
        val servo2 = crickitHat.servo(2, ServoTrim.TOWERPRO_SG90).apply {
            this at SHOULDER_MIN
        }
        ArmServo(servo2, SHOULDER_MIN, SHOULDER_MAX, SHOULDER_DELTA)
    }

    private val elbowServo by lazy {
        val servo3 = crickitHat.servo(3, ServoTrim.TOWERPRO_SG90).apply {
            this at ELBOW_MIN
        }
        ArmServo(servo3, ELBOW_MIN, ELBOW_MAX, ELBOW_DELTA)
    }

    private val gripper by lazy {
        crickitHat.servo(4, ServoTrim.TOWERPRO_SG90).apply {
            this at GRIPPER_OPEN
        }
    }

    fun close() {
        moveToSetPosition(desiredStates[Request.REST])
        waistStepper.release()
    }

    // manage the state of this construct =============================================================================
    private val _currentState = AtomicReference(State.REST)
    var state: State
        get() = _currentState.get()
        private set(s) {
            publishToTopic(STATE_TOPIC, s)
            _currentState.set(s)
        }
    private val busyNow: Boolean
        get() = state == State.BUSY

    // do stuff =======================================================================================================
    fun execute(request: Request, currentButtons: List<Boolean>) {
        when (state) {
            State.BUSY -> {
                // TODO allow for "interrupt" type request to override busy
            }

            State.REST -> {
                if (request == Request.DEPLOY_FRONT) {
                    state = State.BUSY
                    executor.execute {
                        moveToSetPosition(desiredStates[request])
                    }
                }
                if (request == Request.CALIBRATE) {
                    currentCalirationItem.set(0)
                    state = State.CALIBRATION
                }
            }

            State.FRONT -> {
                if (request == Request.REST) {
                    state = State.BUSY
                    executor.execute {
                        moveToSetPosition(desiredStates[request])
                    }
                }
            }

            State.CALIBRATION -> {
                handleCalibration(currentButtons)
            }

            State.GUARDING -> TODO()
        }
    }

    /**
     * Just a silly thing for the gripper integration.
     */
    fun chomp(iterations: Int = 4) {
        for (i in 1..iterations) {
            gripper at GRIPPER_CLOSE
            sleep(100)
            gripper at GRIPPER_OPEN
            sleep(100)
        }
    }

    private fun moveToSetPosition(desiredState: DesiredState?) {
        while (desiredState != null && busyNow) {
            executeWithMinTime(25) {
                // are we at the desired state?
                val shoulderDone = shoulderServo.moveServoTowards(desiredState.shoulderPosition)
                val elbowDone = elbowServo.moveServoTowards(desiredState.elbowPosition)
                val waistDone = moveWaist(desiredState.waistPosition)

                // TODO other joints

                if (shoulderDone && waistDone && elbowDone) {
                    state = desiredState.finalState
                    if (desiredState.finalState == State.REST) waistStepper.release() // "resting" allows for manual fixes
                }
            }
        }
    }

    /**
     * Move stepper in desired direction and maybe hit the target.
     */
    private var stepperPosition = 0
    fun moveWaist(desiredSteps: Int): Boolean {

        if (desiredSteps == stepperPosition) return true

        // start
        if (stepperPosition < desiredSteps) {
            waistStepper.step(StepperMotorInterface.Direction.FORWARD)
            stepperPosition++
        } else {
            waistStepper.step(StepperMotorInterface.Direction.BACKWARD)
            stepperPosition--
        }
        return false
    }

    private class ArmServo(
        val theServo: ServoDevice,
        val minimumDegrees: Int,
        val maximumDegrees: Int,
        val deltaDegrees: Int
    ) {
        /**
         * Figure out where the shoulder is and see if it needs to move or not
         */
        fun moveServoTowards(desiredAngle: Int): Boolean {
            var currentAngle = theServo.angle.roundToInt()
            val diff = desiredAngle - currentAngle

            if (abs(diff) < deltaDegrees) return true

            // apply the delta and see if it goes out of range
            currentAngle = if (diff < 0) max(minimumDegrees, currentAngle - deltaDegrees)
            else min(maximumDegrees, currentAngle + deltaDegrees)

            theServo at currentAngle
            return false
        }
    }

    fun isAt() = listOf(
        stepperPosition,
        shoulderServo.theServo.angle.roundToInt(),
        elbowServo.theServo.angle.roundToInt(),
        gripper.angle.roundToInt() > (GRIPPER_OPEN - 2)
    )

    val currentCalirationItem = AtomicInteger(0)

    // calibration
    fun handleCalibration(currentButtons: List<Boolean>) {
        if (currentButtons[0]) {
            val x = currentCalirationItem.incrementAndGet()
            if (x > 4) currentCalirationItem.set(0)
        } else if (currentButtons[1]) {
            when (currentCalirationItem.get()) {
                0 -> moveWaist(stepperPosition - 1)
                1 -> shoulderServo.moveServoTowards(SHOULDER_MIN)
                2 -> elbowServo.moveServoTowards(ELBOW_MIN)
                3 -> gripper at GRIPPER_OPEN
            }

        } else if (currentButtons[2]) {
            when (currentCalirationItem.get()) {
                0 -> moveWaist(stepperPosition + 1)
                1 -> shoulderServo.moveServoTowards(SHOULDER_MAX)
                2 -> elbowServo.moveServoTowards(ELBOW_MAX)
                3 -> gripper at GRIPPER_CLOSE
                4 -> {
                    state = State.BUSY
                    executor.execute {
                        moveToSetPosition(desiredStates[Request.REST])
                    }
                }
            }
        }
        publishToTopic(STATE_TOPIC, State.CALIBRATION)
    }
}
