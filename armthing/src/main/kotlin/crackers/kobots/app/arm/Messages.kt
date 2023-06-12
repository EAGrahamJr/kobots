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

import java.time.Duration

/**
 * Describes the basic data for an arm joint location.
 * TODO add in radii to try to calculate endpoint coordintes of things
 */
class JointPosition(val angle: Float, val radius: Float = 0f)

/**
 * Describes a location for the arm.
 */
class ArmPosition(
    val waist: JointPosition,
    val shoulder: JointPosition,
    val gripper: JointPosition,
    val elbow: JointPosition
)

/**
 * Current state
 */
class ArmState(val position: ArmPosition, val busy: Boolean = false)

/**
 * Where to go - default is exact movement.
 */
class JointMovement(val angle: Float, val relative: Boolean = false)

val NO_OP = JointMovement(0f, true)

val STD_PAUSE = Duration.ofMillis(15)

/**
 * Describes an action to perform.
 */
class ArmMovement(
    val waist: JointMovement = NO_OP,
    val shoulder: JointMovement = NO_OP,
    val elbow: JointMovement = NO_OP,
    val gripper: JointMovement = NO_OP,
    val stepPause: Duration = STD_PAUSE, // controls "rate of change" to get to the desired position
    val interruptable: Boolean = true
)

/**
 * Do these things.
 */
class ArmRequest(val movements: List<ArmMovement>)
