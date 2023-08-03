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
import kotlin.math.roundToInt

/**
 * Thing that turns. The basic notion is to allow for a "servo" or a "stepper" motor to be used incrementally or
 * absolutely, somewhat interchangeably. Rotation is roughly based on angular _motor_ movement, not necessarily
 * corresponding to real world coordinates.
 */
interface Rotator : Actuator<RotationMovement> {
    override fun move(movement: RotationMovement): Boolean {
        return rotateTo(movement.angle)
    }

    /**
     * Take a "step" towards this destination. Returns `true` if the target has been reached.
     */
    fun rotateTo(angle: Int): Boolean

    operator fun plusAssign(delta: Int) {
        rotateTo(current() + delta)
    }

    operator fun minusAssign(delta: Int) {
        rotateTo(current() - delta)
    }

    operator fun unaryMinus() {
        rotateTo(current() - 1)
    }

    operator fun unaryPlus() {
        rotateTo(current() + 1)
    }

    /**
     * Current location.
     */
    fun current(): Int

    fun Float.almostEquals(another: Float, wibble: Float): Boolean = abs(this - another) < wibble
}

/**
 * Stepper motor with an optional gear ratio. If [reversed] is `true`, the motor is mounted "backwards" relative to
 * desired rotation. Each movement is executed as a single step of the motor. The accuracy of the movement is dependent
 * on rounding errors in the calculation of the number of steps required to reach the destination.
 */
class BasicStepperRotator(
    private val theStepper: StepperMotorInterface,
    gearRatio: Float = 1f,
    reversed: Boolean = false
) : Rotator {

    init {
        require(gearRatio > 0f) { "gearRatio '$gearRatio' must be greater than zero." }
    }

    private val forwardDirection = if (reversed) BACKWARD else FORWARD
    private val backwardDirection = if (reversed) FORWARD else BACKWARD

    fun release() = theStepper.release()

    private val maxSteps = theStepper.stepsPerRotation / gearRatio
    private var stepsLocation: Int = 0

    override fun current(): Int = (360 * stepsLocation / maxSteps).roundToInt()

    override fun rotateTo(angle: Int): Boolean {
        val destinationSteps = (maxSteps * angle / 360).toInt()
        if (destinationSteps == stepsLocation) return true

        // move towards the destination
        if (destinationSteps < stepsLocation) {
            stepsLocation--
            theStepper.step(backwardDirection)
        } else {
            stepsLocation++
            theStepper.step(forwardDirection)
        }
        // are we there yet?
        return destinationSteps == stepsLocation
    }
}

/**
 * Servo, with software limits to prevent over-rotation. Each "step" is controlled by the `deltaDegrees` parameter
 * (`null` means absolute movement, default 1 degree per "step"). Servo movement is done in "whole degrees" (int), so
 * there may be some small rounding errors.
 *
 * The [physicalRange] is the range of the servo _target_ in degrees, and the [servoRange] is the range of the servo
 * itself, in degrees. For example, a servo that rotates its target 180 degrees, but only requires a 90 degree range,
 * would have a [physicalRange] of `0..180` and a [servoRange] of `45..135`. This allows for gear ratios and other
 * mechanical limitations to be accounted for. **NOTE** that the [physicalRange] can be _inverted_ to the [servoRange]
 * if the servo is mounted "backwards" relative to the desired rotation.
 *
 * Note that the target **may** not be exact, due to rounding errors and if the [delta] is large. Example:
 * ```
 * delta = 5
 * current = 47
 * target = 50
 * ```
 * This would indicate that the servo _might not_ move, as the target is within the delta given.
 *
 */
class ServoRotator(
    private val theServo: ServoDevice,
    private val physicalRange: IntRange,
    private val servoRange: IntRange,
    deltaDegrees: Int? = 1
) : Rotator {
    private val delta: Float? = if (deltaDegrees == null) null else abs(deltaDegrees).toFloat()

    private val PRECISION = 0.1f

    private val servoLowerLimit: Float
    private val servoUpperLimit: Float

    init {
        require(physicalRange.first < physicalRange.last) { "physicalRange '$physicalRange' must be increasing" }
        with(servoRange) {
            servoLowerLimit = (if (first < last) first else last).toFloat()
            servoUpperLimit = (if (first < last) last else first).toFloat()
        }
    }

    private fun IntRange.length(): Int = last - first

    // translate an angle in the physical range to a servo angle
    private fun translate(angle: Int): Float {
        val physical = (angle - physicalRange.first).toFloat()
        val servo = physical * servoRange.length() / physicalRange.length()
        return servo + servoRange.first
    }

    // report the current angle, translated to the physical range
    override fun current(): Int = theServo.angle.let {
        val servo = it - servoRange.first
        val physical = servo * physicalRange.length() / servoRange.length()
        physical + physicalRange.first
    }.roundToInt()

    /**
     * Figure out if we need to move or not (and how much)
     */
    override fun rotateTo(angle: Int): Boolean {
        // angle must be in the physical range
        require(angle in physicalRange) { "Angle '$angle' is not in physical range '$physicalRange'." }

        val currentAngle = theServo.angle
        val targetAngle = translate(angle)

        // this is an absolute move without any steps, so set it up and fire it
        if (delta == null) {
            if (reachedTarget(currentAngle, targetAngle, PRECISION)) return true

            // move to the target (trimmed) and we're done
            theServo at trimTargetAngle(targetAngle)
            return true
        }

        // check to see if within the target
        if (targetAngle.almostEquals(currentAngle, delta)) return true

        // apply the delta and make sure it doesn't go out of range (technically it shouldn't)
        val nextAngle = trimTargetAngle(
            if (currentAngle > targetAngle) currentAngle - delta else currentAngle + delta
        )

        // move it and re-check
        theServo at nextAngle
        return reachedTarget(theServo.angle, targetAngle, delta)
    }

    // determine if the servo is at the target angle or at either of the limits
    private fun reachedTarget(servoCurrent: Float, servoTarget: Float, delta: Float): Boolean {
        return servoCurrent.almostEquals(servoTarget, delta) ||
            servoCurrent.almostEquals(servoLowerLimit, PRECISION) ||
            servoCurrent.almostEquals(servoUpperLimit, PRECISION)
    }

    // limit the target angle to the servo limits
    private fun trimTargetAngle(targetAngle: Float): Float {
        return when {
            targetAngle < servoLowerLimit -> servoLowerLimit
            targetAngle > servoUpperLimit -> servoUpperLimit
            else -> targetAngle
        }
    }
}
