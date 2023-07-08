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

package crackers.kobots.app.parts

import com.diozero.api.ServoDevice
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A linear actuator that can be used to extend and retract a part of a machine. Movement is defined as a percentage
 * of the total range of motion. Note that some actuators may not be able to move partially, so the actual movement
 * may be rounded to fully extend or retract (e.g. a solenoid).
 */
abstract class LinearActuator : Actuator<LinearMovement> {
    override fun move(movement: LinearMovement): Boolean {
        val target = if (movement.relative) current() + movement.percentage else movement.percentage
        return extendTo(max(0, min(100, target)))
    }

    abstract fun extendTo(percentage: Int): Boolean
    abstract fun current(): Int
}

/**
 * A linear actuator that uses a servo to extend and retract. Due to rounding errors and whatever gearing is applied,
 * the "step" size is basically however far the servo moves the actuator in one degree of rotation.
 */
class ServoLinearActuator(
    val theServo: ServoDevice,
    val homeDegrees: Float,
    val maximumDegrees: Float
) : LinearActuator() {
    private val servoSwingDegrees = abs(maximumDegrees - homeDegrees)
    private val whichWay = if (maximumDegrees > homeDegrees) 1f else -1f

    override fun extendTo(percentage: Int): Boolean {
        val delta = percentage - current()
        if (delta != 0) {
            val currentAngle = theServo.angle

            if (delta > 0) {
                theServo.angle = currentAngle + whichWay
            } else {
                theServo.angle = currentAngle - whichWay
            }
        }
        return percentage - current() == 0
    }

    override fun current(): Int {
        val degrees = theServo.angle
        return (abs(degrees - homeDegrees) * 100 / servoSwingDegrees).roundToInt()
    }
}
