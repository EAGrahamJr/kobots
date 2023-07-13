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
import crackers.kobots.app.arm.TheArm.GRIPPER_OPEN
import crackers.kobots.app.arm.TheArm.elbow
import crackers.kobots.app.arm.TheArm.extender
import crackers.kobots.app.arm.TheArm.gripper
import crackers.kobots.app.arm.TheArm.homeAction
import crackers.kobots.app.arm.TheArm.mainRoto
import crackers.kobots.app.arm.TheArm.waist
import crackers.kobots.parts.ActionSpeed
import crackers.kobots.parts.sequence

private val ELBOW_TRAVEL = 90f

private val LANDING_WAIST = 90f
private val LANDING_EXTENDER = 50

private val DROPS_ROTO = 90f
private val DROPS_GRAB = 55f
private val DROPS_EXTENDER = 80
private val DROPS_ELBOW = 160f

val getDrops by lazy {
    sequence {
        name = "Get Drops"
        action {
            mainRoto.rotate { angle = DROPS_ROTO }
            requestedSpeed = ActionSpeed.FAST
        }
        action {
            gripper.rotate { angle = GRIPPER_OPEN }
            extender.extend { distance = DROPS_EXTENDER }
        }
        action {
            elbow.rotate { angle = DROPS_ELBOW }
        }
        action {
            gripper.rotate { angle = DROPS_GRAB }
        }
        action {
            elbow.rotate { angle = ELBOW_TRAVEL }
        }
        action {
            waist.rotate { angle = LANDING_WAIST }
            extender.extend { distance = LANDING_EXTENDER }
        }
        action {
            elbow.rotate { angle = ELBOW_DOWN }
        }
        action {
            gripper.rotate { angle = GRIPPER_OPEN }
        }
        action {
            elbow.rotate { angle = ELBOW_UP }
            mainRoto.rotate { angle = 0f }
            requestedSpeed = ActionSpeed.FAST
        }
        this + homeAction
    }
}

val storeDrops by lazy {
    sequence {
        name = "Store Drops"
        action {
            waist.rotate { angle = LANDING_WAIST }
            elbow.rotate { angle = ELBOW_TRAVEL }
            gripper.rotate { angle = GRIPPER_OPEN }
            extender.extend { distance = LANDING_EXTENDER }
        }
        action {
            elbow.rotate { angle = ELBOW_DOWN }
        }
        action {
            gripper.rotate { angle = DROPS_GRAB }
        }
        action {
            elbow.rotate { angle = ELBOW_TRAVEL }
            extender.extend { distance = DROPS_EXTENDER }
        }
        action {
            waist.rotate { angle = 0f }
            mainRoto.rotate { angle = DROPS_ROTO }
            requestedSpeed = ActionSpeed.FAST
        }
        action {
            elbow.rotate { angle = DROPS_ELBOW }
        }
        action {
            gripper.rotate { angle = GRIPPER_OPEN }
        }
        action {
            elbow.rotate { angle = ELBOW_UP }
        }
        action {
            mainRoto.rotate { angle = 0f }
            requestedSpeed = ActionSpeed.FAST
        }
        this + homeAction
    }
}
