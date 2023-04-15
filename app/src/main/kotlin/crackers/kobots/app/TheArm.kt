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
import crackers.kobots.devices.at
import kotlin.math.roundToInt

private class DesiredState(
    val shoulderPosition: Int,
    val finalState: TheArm.ArmState
)

/**
 * First attempt at a controlled "arm-like" structure.
 */
object TheArm {
    enum class ArmState {
        AT_REST, REST, DEPLOY_FRONT, AT_FRONT
    }

    private const val SHOULDER_DELTA = 2
    private const val SHOULDER_START = 90
    private const val SHOUDLER_STOP = 0

    private val desiredStates = mapOf(
        ArmState.DEPLOY_FRONT to (DesiredState(SHOUDLER_STOP, ArmState.AT_FRONT)),
        ArmState.REST to DesiredState(SHOULDER_START, ArmState.AT_REST)
    )

    val shoulderServo by lazy {
        crickitHat.servo(4, ServoTrim.TOWERPRO_SG90).apply {
            this at SHOULDER_START
        }
    }

    fun close() {
        shoulderServo at SHOULDER_START
    }

    fun execute() {
        // are we at the desired state? if not, start moving
        val whatToDo = StatusFlags.armStatus.get()
        desiredStates[whatToDo]?.also {
            val shoulderDone = moveShoulder(it.shoulderPosition)
            // TODO other joints
            if (shoulderDone) StatusFlags.armStatus.set(it.finalState)
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
                    if (shoulderOutOfRange(it)) SHOUDLER_STOP else it
                }
            } else {
                // TODO this is "down"
                (shoulderAngle + SHOULDER_DELTA).let {
                    if (shoulderOutOfRange(it)) SHOULDER_START else it
                }
            }
            shoulderServo at shoulderAngle
        }
        return false
    }

    private fun shoulderOutOfRange(angle: Int): Boolean = angle !in (SHOUDLER_STOP..SHOULDER_START)
}
