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
import java.util.concurrent.atomic.AtomicBoolean
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
    enum class Request {
        NONE, REST, DEPLOY_FRONT, GUARD
    }

    enum class State {
        BUSY, REST, FRONT, GUARDING
    }

    private const val SHOULDER_DELTA = 1
    private const val SHOULDER_MAX = 0  // maximum _elevation_
    private const val SHOUDLER_MIN = 90 // translates to about 60 degrees due to gear ratios

    private const val ELBOW_DELTA = 2
    private const val ELBOW_MAX = 180
    private const val ELBOW_MIN = 90

    private const val MAX_WAIST_STEPS = 200 * 1.29 // 36->12=24->56 = 1:1.29
    private val QUARTER = (MAX_WAIST_STEPS / 4f).roundToInt()

    // shoulder: current configuration is 0 for UP and 90 for down
    // waist: start depends on manual adjustment, but tries to keep a notion of "where it is"
    private val desiredStates = mapOf(
        Request.DEPLOY_FRONT to (DesiredState(State.FRONT, QUARTER, SHOUDLER_MIN, 135)),
        Request.REST to DesiredState(State.REST, 0, SHOULDER_MAX, ELBOW_MAX)
    )

    // hardware! =====================================================================================================
    private val waistStepper by lazy { BasicStepperMotor(200, crickitHat.motorStepperPort()) }

    private val servo2 by lazy {
        crickitHat.servo(2, ServoTrim.TOWERPRO_SG90).apply {
            this at SHOULDER_MAX
        }
    }
    private val shoulderServo by lazy { ArmServo(servo2, SHOULDER_MAX, SHOUDLER_MIN, SHOULDER_DELTA) }

    private val servo3 by lazy {
        crickitHat.servo(3, ServoTrim.TOWERPRO_SG90).apply {
            this at ELBOW_MAX
        }
    }
    private val elbowServo by lazy { ArmServo(servo3, ELBOW_MIN, ELBOW_MAX, ELBOW_DELTA) }

    fun close() {
        servo2 at SHOULDER_MAX
        servo3 at ELBOW_MAX
        waistStepper.release()
    }

    // manage the state of this construct =============================================================================
    private var busyNow = AtomicBoolean(false)
    private val currentState = AtomicReference(State.REST)
    val state: State
        get() = currentState.get()

    // do stuff =======================================================================================================
    fun execute(request: Request) {
        if (busyNow.get()) {
            // TODO allow for "interrupt" type request to override busy
        } else {
            if (request == Request.NONE) return
            busyNow.set(true)
            currentState.set(State.BUSY)
            executor.execute {
                moveToSetPosition(desiredStates[request])
            }
        }
    }

    private fun moveToSetPosition(desiredState: DesiredState?) {
        while (desiredState != null && busyNow.get()) {
            executeWithMinTime(25) {
                // are we at the desired state?
                val shoulderDone = shoulderServo.moveServoTowards(desiredState.shoulderPosition)
                val elbowDone = elbowServo.moveServoTowards(desiredState.elbowPosition)
                val waistDone = moveWaist(desiredState.waistPosition)

                // TODO other joints

                if (shoulderDone && waistDone && elbowDone) {
                    currentState.set(desiredState.finalState)
                    busyNow.set(false)
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
}
