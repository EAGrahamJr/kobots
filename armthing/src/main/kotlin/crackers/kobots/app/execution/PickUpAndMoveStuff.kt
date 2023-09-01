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
import crackers.kobots.app.arm.TheArm.EXTENDER_HOME
import crackers.kobots.app.arm.TheArm.GRIPPER_CLOSED
import crackers.kobots.app.arm.TheArm.GRIPPER_OPEN
import crackers.kobots.app.arm.TheArm.WAIST_HOME
import crackers.kobots.app.arm.TheArm.elbow
import crackers.kobots.app.arm.TheArm.extender
import crackers.kobots.app.arm.TheArm.gripper
import crackers.kobots.app.arm.TheArm.waist
import crackers.kobots.parts.sequence

/**
 * Picks up stuff from the places and returns it.
 */
object PickUpAndMoveStuff {
    private val dropMover = MoveStuffAround(extenderToPickupTarget = 55, elbowForPickupTarget = -7)
    val moveEyeDropsToDropZone = dropMover.moveObjectToTarget()
    val returnDropsToStorage = dropMover.pickupAndReturn()

    private val defaultThinItemMover = MoveStuffAround(
        elbowForPickupTarget = 9,
        closeOnItem = GRIPPER_CLOSED,
        dropOffElbow = ELBOW_DOWN
    )
    val moveThinItemToTarget = defaultThinItemMover.moveObjectToTarget()
    val thinItemReturn = defaultThinItemMover.pickupAndReturn()
}

/**
 * Magic things for transporting stuff.
 */
const val MAGIC_EXTENDER = 15
const val ROTO_PICKUP = "Pick Up From Location"
const val ROTO_RETURN = "Return To Sender"

class MoveStuffAround(
    val closeOnItem: Int = 90, // how much to close the gripper to grab the item - this stresses the technic a bit

    val extenderToPickupTarget: Int = 66, // how far to extend the extender to reach the pickup zone
    val elbowForPickupTarget: Int = 10, // how far to rotate the elbow to reach the pickup zone

    val elbowForTransport: Int = 45,

    val dropOffExtender: Int = 30, // how far to extend the extender to reach the drop off point
    val dropOffElbow: Int = -7 // how far to rotate the elbow to reach the drop off point
) {

    private val backOffAndHome = sequence {
        action {
            extender goTo MAGIC_EXTENDER
        }
        this += homeSequence
    }

    private val pickupFromLocation = sequence {
        this += backOffAndHome

        // avoid collisions!!
        action {
            waist rotate WAIST_HOME
            gripper goTo GRIPPER_OPEN
        }
        action { elbow rotate elbowForPickupTarget }
        action { extender goTo extenderToPickupTarget }

        action { gripper goTo closeOnItem }

        // basically just pick it up a little bit
        action {
            elbow rotate elbowForPickupTarget + 5
            extender goTo MAGIC_EXTENDER
        }

        // reel it in!!!!
        action {
            extender goTo EXTENDER_HOME
            elbow rotate elbowForTransport
        }
    }

    // assumes something is in the gripper
    private val returnToPickupLocation = sequence {
        action { waist rotate WAIST_HOME }
        action { elbow rotate elbowForPickupTarget + 10 }
        action { extender goTo extenderToPickupTarget }
        action { elbow rotate elbowForPickupTarget }
        action { gripper goTo GRIPPER_OPEN }
        this += backOffAndHome
    }

    fun moveObjectToTarget() = sequence {
        name = ROTO_PICKUP
        this += pickupFromLocation
        action { waist rotate 90 }
        action { extender goTo dropOffExtender }
        action { elbow rotate dropOffElbow }
        action { gripper goTo GRIPPER_OPEN }
        action {
            extender goTo EXTENDER_HOME
            elbow rotate elbowForTransport
        }
        this += homeSequence
    }

    fun pickupAndReturn() = sequence {
        name = ROTO_RETURN
        this += backOffAndHome
        action {
            waist rotate 90
            gripper goTo GRIPPER_OPEN
        }
        action {
            extender goTo dropOffExtender - 10
            elbow rotate dropOffElbow
        }
        action { extender goTo dropOffExtender }
        action { gripper goTo closeOnItem }

        action {
            extender goTo EXTENDER_HOME
            elbow rotate elbowForTransport
        }
        this += returnToPickupLocation
    }
}
