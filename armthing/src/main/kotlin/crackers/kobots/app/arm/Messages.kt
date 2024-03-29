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

import crackers.kobots.parts.app.KobotsEvent

/**
 * Describes the basic data for an arm joint location.
 * TODO add in radii to try to calculate endpoint coordintes of things
 */
data class JointPosition(val angle: Int, val radius: Float = 0f)

/**
 * Describes a location for the arm.
 */
data class ArmPosition(
    val waist: JointPosition,
    val extender: JointPosition,
    val gripper: JointPosition,
    val elbow: JointPosition
) {
    fun mapped(): Map<String, Int> = mapOf(
        "WST" to waist.angle,
        "XTN" to extender.angle,
        "ELB" to elbow.angle,
        "GRP" to gripper.angle
    )
}

/**
 * Current state
 */
class ArmState(val position: ArmPosition, val busy: Boolean = false) : KobotsEvent
