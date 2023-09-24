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

import crackers.kobots.app.arm.TheArm.ELBOW_DOWN
import crackers.kobots.app.arm.TheArm.ELBOW_UP
import crackers.kobots.app.arm.TheArm.EXTENDER_FULL
import crackers.kobots.app.arm.TheArm.EXTENDER_HOME
import crackers.kobots.app.arm.TheArm.GRIPPER_CLOSED
import crackers.kobots.app.arm.TheArm.GRIPPER_OPEN
import crackers.kobots.app.arm.TheArm.WAIST_HOME
import crackers.kobots.app.arm.TheArm.WAIST_MAX
import crackers.kobots.app.arm.TheArm.elbow
import crackers.kobots.app.arm.TheArm.extender
import crackers.kobots.app.arm.TheArm.gripper
import crackers.kobots.app.arm.TheArm.waist
import crackers.kobots.parts.movement.ActionSpeed
import crackers.kobots.parts.movement.sequence

/**
 * Home the arm.
 */

/**
 * Wave hello kinda.
 */
val sayHi by lazy {
    sequence {
        name = "Say Hi"
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
        this += homeSequence
    }
}

val excuseMe by lazy {
    sequence {
        name = "Excuse Me"
        this += homeSequence
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
        this += homeSequence
    }
}

val goToSleep by lazy {
    sequence {
        name = "Go To Sleep"
        this += homeSequence
        action {
            waist rotate WAIST_MAX
            requestedSpeed = ActionSpeed.SLOW
        }
        action {
            requestedSpeed = ActionSpeed.VERY_SLOW
            elbow rotate ELBOW_DOWN
            extender goTo 50
        }
    }
}

val homeSequence by lazy {
    sequence {
        name = "Home"
        // make sure the gripper is out of the way of anything
        action { extender goTo EXTENDER_HOME }
        action { elbow rotate ELBOW_UP }
        // now we can close and finish
        action {
            waist rotate WAIST_HOME
            gripper goTo GRIPPER_CLOSED
        }
    }
}
