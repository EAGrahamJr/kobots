/*
 * Copyright 2022-2024 by E. A. Graham, Jr.
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

import crackers.kobots.app.arm.TheArm.ARM_DOWN
import crackers.kobots.app.arm.TheArm.ARM_UP
import crackers.kobots.app.arm.TheArm.BOOM_UP
import crackers.kobots.app.arm.TheArm.GRIPPER_CLOSED
import crackers.kobots.app.arm.TheArm.GRIPPER_OPEN
import crackers.kobots.app.arm.TheArm.SWING_HOME
import crackers.kobots.app.arm.TheArm.armLink
import crackers.kobots.app.arm.TheArm.boomLink
import crackers.kobots.app.arm.TheArm.gripper
import crackers.kobots.app.arm.TheArm.swing
import crackers.kobots.parts.movement.ActionSpeed
import crackers.kobots.parts.movement.sequence

/**
 * Wave hello kinda.
 */
val sayHi by lazy {
    sequence {
        name = "Say Hi"
        action {
            armLink rotate 45
            boomLink rotate 45
            gripper goTo GRIPPER_OPEN
            swing rotate 90
        }
        (1..4).forEach {
            action {
                armLink rotate ARM_UP
                requestedSpeed = ActionSpeed.FAST
            }
            action {
                armLink rotate 30
                requestedSpeed = ActionSpeed.FAST
            }
        }
        this += homeSequence
    }
}

/**
 * Get attention
 */
val excuseMe by lazy {
    sequence {
        name = "Excuse Me"
        this += homeSequence
        action {
            swing rotate 90
            armLink rotate 45
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

/**
 * Kinda sorta sleepy
 */
val armSleep by lazy {
    sequence {
        name = "Go To Sleep"
        this += homeSequence
//        action {
//            waist rotate WAIST_MAX
//            requestedSpeed = ActionSpeed.SLOW
//        }
//        action {
//            requestedSpeed = ActionSpeed.VERY_SLOW
//            elbow rotate ELBOW_DOWN
//            extender goTo 50
//        }
    }
}

/**
 * Send it home - should be "0" state for servos.
 */
val homeSequence by lazy {
    sequence {
        name = "Home"
        // make sure the gripper is out of the way of anything
        action {
            boomLink rotate BOOM_UP
            gripper goTo GRIPPER_OPEN
        }
        action {
            armLink rotate ARM_DOWN
            swing rotate SWING_HOME
        }
    }
}
