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

import crackers.kobots.app.KobotsAction
import crackers.kobots.app.KobotsEvent
import crackers.kobots.app.arm.TheArm.GO_HOME
import java.time.Duration

/**
 * Describes the basic data for an arm joint location.
 * TODO add in radii to try to calculate endpoint coordintes of things
 */
data class JointPosition(val angle: Float, val radius: Float = 0f)

/**
 * Describes a location for the arm.
 */
data class ArmPosition(
    val waist: JointPosition,
    val shoulder: JointPosition,
    val gripper: JointPosition,
    val elbow: JointPosition
)

/**
 * Current state
 */
class ArmState(val position: ArmPosition, val busy: Boolean = false) : KobotsEvent

/**
 * Where to go - default is exact movement. An [stopCheck] function may also be supplied to terminate movement prior
 * to reaching the desired [angle]
 */
class JointMovement(val angle: Float, val relative: Boolean = false, val stopCheck: () -> Boolean = { false })

val NO_OP = JointMovement(0f, true) { true }

val STD_PAUSE = Duration.ofMillis(15)

interface ArmRequest : KobotsAction

/**
 * Describes an action to perform.
 */
class ArmMovement(
    val waist: JointMovement = NO_OP,
    val shoulder: JointMovement = NO_OP,
    val elbow: JointMovement = NO_OP,
    val gripper: JointMovement = NO_OP,
    val stepPause: Duration = STD_PAUSE, // controls "rate of change" to get to the desired position
    override val interruptable: Boolean = true
) : ArmRequest

/**
 * Do these things.
 */
class ArmSequence(vararg val movements: ArmMovement, override val interruptable: Boolean = true) : ArmRequest

val armPark = ArmSequence(GO_HOME)
