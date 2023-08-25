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

import crackers.kobots.app.arm.TheArm
import crackers.kobots.app.arm.TheArm.ELBOW_DOWN
import crackers.kobots.app.arm.TheArm.EXTENDER_FULL
import crackers.kobots.app.arm.TheArm.GRIPPER_CLOSED
import crackers.kobots.app.arm.TheArm.GRIPPER_OPEN
import crackers.kobots.app.arm.TheArm.WAIST_MAX
import crackers.kobots.app.arm.TheArm.elbow
import crackers.kobots.app.arm.TheArm.extender
import crackers.kobots.app.arm.TheArm.gripper
import crackers.kobots.app.arm.TheArm.waist
import crackers.kobots.parts.ActionBuilder
import crackers.kobots.parts.ActionSpeed
import crackers.kobots.parts.sequence

/**
 * Home the arm.
 */
val homeAction = ActionBuilder().apply {
    waist rotate TheArm.WAIST_HOME
    extender goTo TheArm.EXTENDER_HOME
    elbow rotate TheArm.ELBOW_UP
    gripper goTo GRIPPER_CLOSED
}

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

val goToSleep by lazy {
    sequence {
        name = "Go To Sleep"
        this + homeAction
        action {
            waist rotate WAIST_MAX
        }
        action {
            requestedSpeed = ActionSpeed.SLOW
            elbow rotate ELBOW_DOWN
        }
    }
}

val homeSequence = sequence {
    this + homeAction
}
