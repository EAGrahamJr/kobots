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

import com.diozero.api.ServoTrim
import com.diozero.devices.sandpit.motor.BasicStepperMotor
import com.diozero.devices.sandpit.motor.StepperMotorInterface
import crackers.kobots.devices.at
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

private class DesiredState(
    val shoulderPosition: Int,
    val finalState: TheArm.State
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

    private const val SHOULDER_DELTA = 2
    private const val SHOULDER_MAX = 90
    private const val SHOUDLER_MIN = 0

    private val desiredStates = mapOf(
        Request.DEPLOY_FRONT to (DesiredState(SHOUDLER_MIN, State.FRONT)),
        Request.REST to DesiredState(SHOULDER_MAX, State.REST)
    )

    private val waistStepper by lazy { BasicStepperMotor(200, crickitHat.motorStepperPort()) }

    private val shoulderServo by lazy {
        crickitHat.servo(4, ServoTrim.TOWERPRO_SG90).apply {
            this at SHOULDER_MAX
        }
    }

    fun close() {
        shoulderServo at SHOULDER_MAX
        waistStepper.release()
    }

    private var whatToDo: Request? = null
    private var busyNow = false
    private var currentState = State.REST
    val state: State
        get() = currentState

    fun execute(request: Request) {
        if (busyNow) {
            // TODO allow for "interrupt" type request to override busy
        } else {
            if (request == Request.NONE) return
            busyNow = true
            currentState = State.BUSY
            whatToDo = request
        }

        // are we at the desired state? if not, start moving
        desiredStates[whatToDo]?.also {
            val shoulderDone = moveShoulder(it.shoulderPosition)

            // TODO other joints - this just kinda spins the turntable without real regard to the arm position
            if (!waistDone) waister()
            if (shoulderDone && waistDone) {
                waistDone = false
                currentState = it.finalState
                busyNow = false
            }
        }
    }

    // shoulder =======================================================================================================
    // TODO current configuration is 0 for UP and 90 for down, so the math is kind of backwards

    /**
     * Figure out where the shoulder is and see if it needs to move or not
     */
    private fun moveShoulder(desiredAngle: Int): Boolean {
        var shoulderAngle = shoulderServo.angle.roundToInt()
        if (shoulderAngle == desiredAngle) return true

        // figure out if we're going up or down based on desired state and where we're at
        (desiredAngle - shoulderAngle).also { diff ->
            // apply the delta and see if it goes out of range
            shoulderAngle = if (diff < 0) {
                // TODO this is "up"
                (shoulderAngle - SHOULDER_DELTA).let {
                    if (shoulderOutOfRange(it)) SHOUDLER_MIN else it
                }
            } else {
                // TODO this is "down"
                (shoulderAngle + SHOULDER_DELTA).let {
                    if (shoulderOutOfRange(it)) SHOULDER_MAX else it
                }
            }
            shoulderServo at shoulderAngle
        }
        return false
    }

    private fun shoulderOutOfRange(angle: Int): Boolean = angle !in (SHOUDLER_MIN..SHOULDER_MAX)

    // TODO temporary stepper function
    private var flagDirection = false
    private val MAX_STEPS = 155 // 36->12=24->56 = 1:1.29
    private val QUARTER = (MAX_STEPS / 4f).roundToInt()
    private var waistDone = false
    val stepperPosition = AtomicInteger(0)
    fun waister() {
        // start
        if (stepperPosition.get() == 0 || flagDirection) {
            waistStepper.step(StepperMotorInterface.Direction.FORWARD)
            val newPosition = stepperPosition.incrementAndGet()
            flagDirection = newPosition < QUARTER
            waistDone = newPosition == QUARTER
        } else {
            waistStepper.step(StepperMotorInterface.Direction.BACKWARD)
            val newPosition = stepperPosition.decrementAndGet()
            // if this "transitions" to our "zero" base, let the stepper go
            if (newPosition <= 0) waistStepper.release()
            waistDone = newPosition == 0
        }
    }
}
