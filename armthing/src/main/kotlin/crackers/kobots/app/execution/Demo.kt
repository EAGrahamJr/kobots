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
import crackers.kobots.app.arm.TheArm.GRIPPER_CLOSED
import crackers.kobots.app.arm.TheArm.GRIPPER_OPEN
import crackers.kobots.app.arm.TheArm.WAIST_HOME
import crackers.kobots.app.arm.TheArm.elbow
import crackers.kobots.app.arm.TheArm.extender
import crackers.kobots.app.arm.TheArm.gripper
import crackers.kobots.app.arm.TheArm.homeAction
import crackers.kobots.app.arm.TheArm.waist
import crackers.kobots.parts.ActionSpeed
import crackers.kobots.parts.sequence

/*
 * Demonstration type sequences for V3, and V4.
 */

private const val LOAD_EXTENDER = 65
private const val LOAD_GRIPPER = 90
private const val ELBOW_FOR_DROPS = -5
private const val TRAVEL_ELBOW = 45
private const val MIDPOINT_WAIST = 45
private const val TARGET_WAIST = 90
private const val TARGET_EXTENDER = 30

fun Int.retract() = (this * .66).toInt()

var hasPickedUpEyeDrops = false

/**
 * Pick up something from the starting point and deliver it to the exit point.
 */
val pickAndMove by lazy {
    sequence {
        name = "Pick up drops"
        this + homeAction

        action {
            elbow rotate ELBOW_FOR_DROPS
            gripper goTo GRIPPER_OPEN
            extender goTo TARGET_EXTENDER.retract()
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
            waist rotate TARGET_WAIST
            elbow rotate TRAVEL_ELBOW
        }
        action {
            extender goTo TARGET_EXTENDER
            elbow rotate ELBOW_FOR_DROPS
        }
        action { gripper goTo GRIPPER_OPEN }
        action {
            extender goTo TARGET_EXTENDER.retract()
            elbow rotate {
                angle = TRAVEL_ELBOW
                stopCheck = {
                    hasPickedUpEyeDrops = true
                    false
                }
            }
        }
        this + homeAction
    }
}

/**
 * Returns the thing from above.
 */
val returnTheThing by lazy {
    sequence {
        name = "Return the drops"
        this + homeAction
        action {
            waist rotate TARGET_WAIST
            extender goTo TARGET_EXTENDER.retract()
            elbow rotate TRAVEL_ELBOW
            gripper goTo GRIPPER_OPEN
//            requestedSpeed = ActionSpeed.FAST
        }
        action {
            elbow rotate ELBOW_FOR_DROPS
        }
        action {
            extender goTo TARGET_EXTENDER
        }
        action { gripper goTo LOAD_GRIPPER }
        action { elbow rotate TRAVEL_ELBOW }
        action {
            waist rotate WAIST_HOME
            extender goTo LOAD_EXTENDER.retract()
        }
        action {
            elbow rotate ELBOW_FOR_DROPS
            extender goTo LOAD_EXTENDER
        }
        action { gripper goTo GRIPPER_OPEN }
        action {
            elbow rotate {
                angle = TRAVEL_ELBOW
                stopCheck = {
                    hasPickedUpEyeDrops = false
                    false
                }
            }
            extender goTo LOAD_EXTENDER.retract()
        }
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
            elbow rotate 45
            extender goTo EXTENDER_FULL
            gripper goTo GRIPPER_OPEN
            waist rotate 90
        }
        (1..4).forEach {
            action {
                elbow rotate 80
                requestedSpeed = ActionSpeed.FAST
            }
            action {
                elbow rotate 30
                requestedSpeed = ActionSpeed.FAST
            }
        }
        this + homeAction
    }
}

val excuseMe by lazy {
    sequence {
        name = "Excuse Me"
        this + homeAction
        action {
            waist rotate 90
            elbow rotate 45
        }
        (1..5).forEach {
            action {
                gripper goTo GRIPPER_OPEN
                requestedSpeed = ActionSpeed.FAST
            }
            action {
                gripper goTo GRIPPER_CLOSED
                requestedSpeed = ActionSpeed.FAST
            }
        }
        this + homeAction
    }
}
