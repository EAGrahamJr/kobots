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
import crackers.kobots.app.TheArm.ShoulderStatus.*
import crackers.kobots.devices.at

/**
 * TODO fill this in
 */
object TheArm {
    enum class ArmState {
        AT_REST, REST, DEPLOY_FRONT, AT_FRONT
    }

    val shoulderServo by lazy {
        crickitHat.servo(4, ServoTrim.TOWERPRO_SG90).apply {
            angle = START_ANGLE
        }
    }

    fun close() {
        shoulderServo at START_ANGLE
    }

    fun execute() {
        // are we at the desired state? if not, start moving
        when (StatusFlags.armStatus.get()) {
            ArmState.DEPLOY_FRONT -> {
                val shoulderDone = moveShoulder(UP)
                if (shoulderDone) StatusFlags.armStatus.set(ArmState.AT_FRONT)
            }

            ArmState.REST -> {
                val shoulderDone = moveShoulder(DOWN)
                if (shoulderDone) StatusFlags.armStatus.set(ArmState.AT_REST)
            }

            else -> {}
        }
    }

    // shoulder =======================================================================================================
    const val DELTA = -10f
    const val START_ANGLE = 90f
    const val STOP_ANGLE = 0f

    private enum class ShoulderStatus {
        DOWN, UP, MOVING_UP, MOVING_DOWN
    }

    private var shoulderState = DOWN

    private fun moveShoulder(whereIWantToBe: ShoulderStatus): Boolean {
        val whatIshouldBeDoing = if (whereIWantToBe == UP) MOVING_UP else MOVING_DOWN
        val notWhereIWantToBe = if (whereIWantToBe == UP) DOWN else UP

        if (shoulderState == whereIWantToBe) return true
        if (shoulderState == notWhereIWantToBe) shoulderState = whatIshouldBeDoing

        if (shoulderState == MOVING_UP) {
            swingUp()
            if (servoAngle == STOP_ANGLE) shoulderState = UP
        } else if (shoulderState == MOVING_DOWN) {
            swingDown()
            if (servoAngle == START_ANGLE) shoulderState = DOWN
        }
        return false
    }

    private var servoAngle = START_ANGLE
    private fun rangeCheck(angle: Float): Boolean =
        angle !in if (START_ANGLE < STOP_ANGLE) {
            (START_ANGLE..STOP_ANGLE)
        } else {
            (STOP_ANGLE..START_ANGLE)
        }

    private fun swingUp() {
        servoAngle = (servoAngle + DELTA).let {
            if (rangeCheck(it)) STOP_ANGLE else it
        }

        shoulderServo at servoAngle
    }

    private fun swingDown() {
        servoAngle = (servoAngle - DELTA).let {
            if (rangeCheck(it)) START_ANGLE else it
        }
        shoulderServo at servoAngle
    }
}
