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

package crackers.kobots.parts

import com.diozero.api.ServoDevice
import com.diozero.devices.sandpit.motor.StepperMotorInterface
import com.diozero.devices.sandpit.motor.StepperMotorInterface.Direction.BACKWARD
import com.diozero.devices.sandpit.motor.StepperMotorInterface.Direction.FORWARD
import crackers.kobots.devices.at
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Thing that turns. The basic notion is to allow for a "servo" or a "stepper" motor to be used incrementally or
 * absolutely, somewhat interchangeably. Rotation is roughly based on angular _motor_ movement, not necessarily
 * corresponding to real world coordinates.
 */
interface Rotator : Actuator<RotationMovement> {
    override fun move(movement: RotationMovement): Boolean {
        val target = if (movement.relative) current() + movement.angle else movement.angle
        return rotateTo(target)
    }

    /**
     * Take a "step" towards this destination. Returns `true` if the target has been reached.
     */
    fun rotateTo(angle: Float): Boolean

    /**
     * Current location.
     */
    fun current(): Float

    fun Float.almostEquals(another: Float, wibble: Float): Boolean = abs(this - another) < wibble
}

/**
 * Stepper motor with an optional gear ratio. If [reversed] is `true`, the motor is mounted "backwards" relative to
 * desired rotation. Each movement is executed as a single step of the motor. The accuracy of the movement is dependent
 * on rounding errors in the calculation of the number of steps required to reach the destination.
 */
class RotatorStepper(
    val theStepper: StepperMotorInterface,
    gearRatio: Float = 1f,
    reversed: Boolean = false
) : Rotator {

    private val forwardDirection = if (reversed) BACKWARD else FORWARD
    private val backwardDirection = if (reversed) FORWARD else BACKWARD

    fun release() = theStepper.release()

    private val maxSteps = theStepper.stepsPerRotation * gearRatio

    internal var currentLocation = 0 // in steps

    override fun current(): Float = 360 * currentLocation / maxSteps

    override fun rotateTo(angle: Float): Boolean {
        val destinationSteps = (maxSteps * angle / 360).roundToInt()
        if (destinationSteps == currentLocation) return true

        // move towards the destination
        if (destinationSteps < currentLocation) {
            currentLocation--
            theStepper.step(backwardDirection)
        } else {
            currentLocation++
            theStepper.step(forwardDirection)
        }
        // are we there yet?
        return destinationSteps == currentLocation
    }
}

/**
 * Servo, with software limits to prevent over-rotation. Movement "speed" is controlled by the `deltaDegrees` parameter
 * (`null` means absolute movement). The [precision] is used for floating-point comparisons.
 *
 * Note that the target **may** not be exact, due to rounding errors and if the [delta] is large. Example:
 * ```
 * delta = 5
 * current = 47
 * target = 50
 * ```
 * This would indicate that the servo would **not** move, as the target is within the delta given.
 *
 * **NOTE** [homeDegrees] is defined as the "zero" point for the servo, and [maximumDegrees] is the _absolute_ maximum
 * position. Thus, the maximum may be **less** than the home position. The [delta] is always a positive number and
 * the actual movement is computed relative to home and maximum.
 *
 * TODO map home/maximum to real-world coordinates
 */
class RotatorServo(
    val theServo: ServoDevice,
    val homeDegrees: Float,
    val maximumDegrees: Float,
    deltaDegrees: Float? = null,
    val precision: Float = .1f
) : Rotator {
    val delta: Float? = if (deltaDegrees == null) null else abs(deltaDegrees)

    override fun current(): Float = theServo.angle

    /**
     * Figure out if we need to move or not (and how much)
     */
    override fun rotateTo(angle: Float): Boolean {
        val currentAngle = current()

        // this is an absolute move without any steps, so set it up and fire it
        val (nextAngle, moveDone) = if (delta == null) {
            val next = withinBounds(angle)
            Pair(next, next.almostEquals(currentAngle, precision))
        } else {
            if (angle.almostEquals(currentAngle, precision)) return true

            // apply the delta and see if it goes out of range
            var nextAngle = if (currentAngle > angle) {
                currentAngle - delta
            } else {
                currentAngle + delta
            }
            nextAngle = withinBounds(nextAngle)

            // if we're at the limits of the servo, we can't move more - so we're done
            val moveDone =
                nextAngle.almostEquals(homeDegrees, precision) || nextAngle.almostEquals(maximumDegrees, precision)

            Pair(nextAngle, moveDone)
        }

        theServo at nextAngle

        // if we're done, we're done
        return moveDone || angle.almostEquals(currentAngle, precision)
    }

    private fun withinBounds(angle: Float) =
        // this is backwards
        if (homeDegrees > maximumDegrees) {
            min(homeDegrees, max(angle, maximumDegrees))
        } else {
            min(maximumDegrees, max(angle, homeDegrees))
        }
}
