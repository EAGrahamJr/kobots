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

package crackers.kobots.app.execution

import crackers.kobots.app.arm.TheArm.ELBOW_UP
import crackers.kobots.app.arm.TheArm.EXTENDER_FULL
import crackers.kobots.app.arm.TheArm.EXTENDER_HOME
import crackers.kobots.app.arm.TheArm.GRIPPER_OPEN
import crackers.kobots.app.arm.TheArm.WAIST_HOME
import crackers.kobots.app.arm.TheArm.elbow
import crackers.kobots.app.arm.TheArm.extender
import crackers.kobots.app.arm.TheArm.gripper
import crackers.kobots.app.arm.TheArm.homeAction
import crackers.kobots.app.arm.TheArm.waist
import crackers.kobots.parts.ActionSpeed
import crackers.kobots.parts.sequence
import kotlin.math.roundToInt

/*
 * Demonstration type sequences for V3, and V4.
 */

private const val LOAD_EXTENDER = 65
private const val LOAD_GRIPPER = 90
private const val ELBOW_FLAT = -5f
private const val TRAVEL_ELBOW = 45f
private const val MIDPOINT_WAIST = 45f
private const val DROP_WAIST = 90f
private const val DROP_EXTENDER = 50

/**
 * Pick up something from the starting point and deliver it to the exit point.
 */
val pickAndMove by lazy {
    sequence {
        name = "Pick and Move"
        this + homeAction

        action {
            elbow rotate ELBOW_FLAT
            gripper goTo GRIPPER_OPEN
        }
        action { extender goTo LOAD_EXTENDER }
        action { gripper goTo LOAD_GRIPPER }
        action { elbow rotate TRAVEL_ELBOW }
        action {
            waist rotate MIDPOINT_WAIST
            extender goTo EXTENDER_HOME
            elbow rotate ELBOW_UP
        }
        action {
            waist rotate DROP_WAIST
            elbow rotate TRAVEL_ELBOW
        }
        action {
            extender goTo DROP_EXTENDER
            elbow rotate ELBOW_FLAT
        }
        action { gripper goTo GRIPPER_OPEN }
        action { elbow rotate TRAVEL_ELBOW }
        this + homeAction
    }
}

/**
 * Returns the thing from above.
 */
val returnTheThing by lazy {
    sequence {
        name = "Return the Thing"
        this + homeAction
        action {
            waist rotate DROP_WAIST
            extender goTo (DROP_EXTENDER * .75).roundToInt()
            elbow rotate ELBOW_FLAT
            gripper goTo GRIPPER_OPEN
            requestedSpeed = ActionSpeed.FAST
        }
        action { extender goTo DROP_EXTENDER }
        action { gripper goTo LOAD_GRIPPER }
        action { elbow rotate TRAVEL_ELBOW }
        action {
            waist rotate WAIST_HOME
            extender goTo DROP_EXTENDER
        }
        action { elbow rotate ELBOW_FLAT }
        action { gripper goTo GRIPPER_OPEN }
        action { elbow rotate TRAVEL_ELBOW }
        this + homeAction
    }
}

/**
 * Wave hello kinda.
 */
val sayHi by lazy {
    sequence {
        name = "Say Hi"
        this + homeAction
        action {
            elbow rotate 45f
            extender goTo EXTENDER_FULL
            gripper goTo GRIPPER_OPEN
            waist rotate 90f
        }
        (1..4).forEach {
            action {
                elbow rotate 80f
                requestedSpeed = ActionSpeed.FAST
            }
            action {
                elbow rotate 30f
                requestedSpeed = ActionSpeed.FAST
            }
        }
        this + homeAction
    }
}
