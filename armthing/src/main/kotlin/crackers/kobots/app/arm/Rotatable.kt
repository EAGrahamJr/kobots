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

package crackers.kobots.app.arm

import com.diozero.api.ServoDevice
import com.diozero.devices.sandpit.motor.StepperMotorInterface
import com.diozero.devices.sandpit.motor.StepperMotorInterface.Direction.BACKWARD
import com.diozero.devices.sandpit.motor.StepperMotorInterface.Direction.FORWARD
import crackers.kobots.app.almostEquals
import crackers.kobots.devices.at
import crackers.kobots.utilities.KobotSleep
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Thing that turns.
 */
interface Rotatable {
    /**
     * Absolute destination. Returns `true` if the target has been reached.
     */
    fun moveTowards(angle: Float): Boolean

    /**
     * Current location.
     */
    fun current(): Float
}

/**
 * Servo with "absolute" positioning - e.g. not related to real world degrees.
 *
 * TODO add gear ratio to this and use it to calculate real world degrees
 */
internal class ArmServo(
    val theServo: ServoDevice,
    val minimumDegrees: Float,
    val maximumDegrees: Float,
    val deltaDegrees: Float? = null // also used as "margin of error"
) : Rotatable {

    override fun current(): Float = theServo.angle

    /**
     * Figure out if we need to move or not (and how much)
     */
    override fun moveTowards(angle: Float): Boolean {
        val currentAngle = current()

        // this is an absolute move without any steps, so set it up and fire it
        val (nextAngle, moveDone) = if (deltaDegrees == null) {
            val next = min(maximumDegrees, max(minimumDegrees, angle))
            Pair(next, next.almostEquals(currentAngle, .1f))
        } else {
            val diff = angle - currentAngle

            // if we've reached the endpoint, we're done
            if (abs(diff) < deltaDegrees) return true

            // apply the delta and see if it goes out of range
            var nextAngle = if (diff < 0) {
                currentAngle - deltaDegrees
            } else {
                currentAngle + deltaDegrees
            }
            nextAngle = withinBounds(nextAngle)

            // if we're at the limits of the servo, we can't move more - so we're done
            val moveDone = (
                nextAngle.almostEquals(minimumDegrees, deltaDegrees / 3) ||
                    nextAngle.almostEquals(maximumDegrees, deltaDegrees / 3)
                )
            Pair(nextAngle, moveDone)
        }

        theServo at nextAngle
        return moveDone
    }

    private fun withinBounds(angle: Float) =
        // this is backwards
        if (minimumDegrees > maximumDegrees) {
            min(minimumDegrees, max(angle, maximumDegrees))
        } else {
            min(maximumDegrees, max(angle, minimumDegrees))
        }
}

/**
 * Stepper motor with a gear ratio.
 */
internal class ArmStepper(
    val theStepper: StepperMotorInterface,
    val gearRatio: Float,
    reversed: Boolean = false
) : Rotatable {

    private val forwardDirection = if (reversed) BACKWARD else FORWARD
    private val backwardDirection = if (reversed) FORWARD else BACKWARD

    fun release() = theStepper.release()

    private val maxSteps = theStepper.stepsPerRotation * gearRatio
    private val stepsPerAngle = min(maxSteps / 360, 1f).roundToInt()

    internal var currentLocation = 0 // in steps

    override fun current(): Float = 360 * currentLocation / maxSteps

    override fun moveTowards(angle: Float): Boolean {
        val destinationSteps = (maxSteps * angle / 360).roundToInt()
        if (destinationSteps == currentLocation) return true

        // move backwards
        (1..stepsPerAngle).forEach {
            if (destinationSteps < currentLocation) {
                currentLocation--
                theStepper.step(backwardDirection)
            } else {
                currentLocation++
                theStepper.step(forwardDirection)
            }
            KobotSleep.millis(1)
        }
        return false
    }
}
