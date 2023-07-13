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
private const val ELBOW_FLAT = 170f
private const val LOAD_GRIPPER = 60f
private const val TRAVEL_ELBOW = 90f
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
            elbow.rotate { angle = ELBOW_FLAT }
            gripper.rotate { angle = GRIPPER_OPEN }
        }
        action {
            extender.extend { distance = LOAD_EXTENDER }
        }
        action {
            gripper.rotate { angle = LOAD_GRIPPER }
        }
        action {
            elbow.rotate { angle = TRAVEL_ELBOW }
        }
        action {
            waist.rotate { angle = MIDPOINT_WAIST }
            extender.extend { distance = 0 }
            elbow.rotate { angle = ELBOW_UP }
        }
        action {
            waist.rotate { angle = DROP_WAIST }
            elbow.rotate { angle = TRAVEL_ELBOW }
        }
        action {
            extender.extend { distance = DROP_EXTENDER }
            elbow.rotate { angle = ELBOW_FLAT }
        }
        action {
            gripper.rotate { angle = GRIPPER_OPEN }
        }
        action {
            elbow.rotate { angle = TRAVEL_ELBOW }
        }
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
            waist.rotate { angle = DROP_WAIST }
            extender.extend { distance = (DROP_EXTENDER * .75).roundToInt() }
            elbow.rotate { angle = ELBOW_FLAT }
            gripper.rotate { angle = GRIPPER_OPEN }
            requestedSpeed = ActionSpeed.FAST
        }
        action {
            extender.extend { distance = DROP_EXTENDER }
        }
        action {
            gripper.rotate { angle = LOAD_GRIPPER }
        }
        action {
            elbow.rotate { angle = TRAVEL_ELBOW }
        }
        action {
            waist.rotate { angle = WAIST_HOME }
            extender.extend { distance = DROP_EXTENDER }
        }
        action {
            elbow.rotate { angle = ELBOW_FLAT }
        }
        action {
            gripper.rotate { angle = GRIPPER_OPEN }
        }
        action {
            elbow.rotate { angle = TRAVEL_ELBOW }
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
            elbow.rotate { angle = 45f }
            extender.extend { distance = 100 }
            gripper.rotate { angle = GRIPPER_OPEN }
            waist.rotate { angle = 90f }
            requestedSpeed = ActionSpeed.FAST
        }
        (1..4).forEach {
            action {
                waist.rotate { angle = 105f }
                requestedSpeed = ActionSpeed.SLOW
            }
            action {
                waist.rotate { angle = 75f }
                requestedSpeed = ActionSpeed.SLOW
            }
        }
        this + homeAction
    }
}
