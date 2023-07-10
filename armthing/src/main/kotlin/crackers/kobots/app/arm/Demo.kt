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

import crackers.kobots.app.arm.TheArm.ELBOW_DOWN
import crackers.kobots.app.arm.TheArm.ELBOW_UP
import crackers.kobots.app.arm.TheArm.GRIPPER_OPEN
import crackers.kobots.app.arm.TheArm.WAIST_HOME
import crackers.kobots.app.arm.TheArm.elbow
import crackers.kobots.app.arm.TheArm.extender
import crackers.kobots.app.arm.TheArm.gripper
import crackers.kobots.app.arm.TheArm.homeAction
import crackers.kobots.app.arm.TheArm.waist
import crackers.kobots.app.bus.ActionSpeed
import crackers.kobots.app.bus.sequence

/*
 * Demonstration type sequences for V3, and V4.
 */


private val EXTENDER_MIDMOVE = 50
private val EXTENDER_DROP = 75
private val ELBOW_MIDMOVE = 90f
private val GRIPPER_GRAB = 55f
private val WAIST_HALFWAY = 45f
private val WAIST_ALLTHEWAY = 90f

/**
 * Pick up something from the starting point and deliver it to the exit point.
 */
val pickAndMove by lazy {
    sequence {
        this + homeAction

        action {
            elbow.rotate { angle = ELBOW_MIDMOVE }
            extender.extend { distance = EXTENDER_MIDMOVE }
        }
        action {
            gripper.rotate { angle = GRIPPER_OPEN }
        }
        action {
            extender.extend { distance = 100 }
            elbow.rotate { angle = ELBOW_DOWN }
        }
        action {
            gripper.rotate { angle = GRIPPER_GRAB }
        }
        action {
            elbow.rotate { angle = ELBOW_MIDMOVE }
        }
        action {
            waist.rotate { angle = WAIST_HALFWAY }
            extender.extend { distance = EXTENDER_MIDMOVE }
        }
        action {
            elbow.rotate { angle = ELBOW_UP }
        }
        action {
            waist.rotate { angle = WAIST_ALLTHEWAY }
            elbow.rotate { angle = ELBOW_MIDMOVE }
        }
        action {
            extender.extend { distance = EXTENDER_DROP }
            elbow.rotate { angle = ELBOW_DOWN }
        }
        action {
            gripper.rotate { angle = GRIPPER_OPEN }
        }
        action {
            elbow.rotate { angle = ELBOW_MIDMOVE }
        }
        this + homeAction
    }
}

val returnTheThing by lazy {
    sequence {
        this + homeAction
        action {
            waist.rotate { angle = WAIST_ALLTHEWAY }
            extender.extend { distance = EXTENDER_DROP - 5 }
            elbow.rotate { angle = ELBOW_MIDMOVE }
            gripper.rotate { angle = GRIPPER_OPEN }
        }
        action {
            elbow.rotate { angle = ELBOW_DOWN }
        }
        action {
            extender.extend { distance = EXTENDER_DROP }
        }
        action {
            gripper.rotate { angle = GRIPPER_GRAB }
        }
        action {
            elbow.rotate { angle = ELBOW_MIDMOVE }
        }
        action {
            waist.rotate { angle = WAIST_HOME }
            extender.extend { distance = 100 }
        }
        action {
            elbow.rotate { angle = ELBOW_DOWN }
        }
        action {
            gripper.rotate { angle = GRIPPER_OPEN }
        }
        action {
            elbow.rotate { angle = ELBOW_MIDMOVE }
        }
        this + homeAction
    }
}

/**
 * Wave hello kinda.
 */
val sayHi by lazy {
    sequence {
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
