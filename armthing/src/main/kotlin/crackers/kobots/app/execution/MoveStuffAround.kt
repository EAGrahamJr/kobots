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

import crackers.kobots.app.arm.TheArm.EXTENDER_HOME
import crackers.kobots.app.arm.TheArm.GRIPPER_OPEN
import crackers.kobots.app.arm.TheArm.elbow
import crackers.kobots.app.arm.TheArm.extender
import crackers.kobots.app.arm.TheArm.gripper
import crackers.kobots.app.arm.TheArm.waist
import crackers.kobots.parts.movement.sequence

/**
 * Defines how to pick something up from a place and put it somewhere else, with the reverse also available.
 *
 * TODO rethink the approach and grab/drop motions: more interim steps?
 */
class MoveStuffAround(
    val closeOnItem: Int = 90, // how much to close the gripper to grab the item - this stresses the technic a bit

    val extenderToPickupTarget: Int = 75, // how far to extend the extender to reach the pickup zone
    val elbowForPickupTarget: Int = 0, // how far to rotate the elbow to reach the pickup zone
    val waistForPickupTarget: Int = 0, // how far to rotate the waist to reach the pickup zone

    val elbowForTransport: Int = 45,

    val dropOffExtender: Int = 75, // how far to extend the extender to reach the drop-off point
    val dropOffElbow: Int = 0, // how far to rotate the elbow to reach the drop-off point
    val dropOffWaist: Int = 90 // how far to rotate the waist to reach the drop-off point
) {

    private val pickupFromLocation = sequence {
        this += homeSequence

        // avoid collisions!!
        action {
            waist rotate waistForPickupTarget
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
        action { waist rotate waistForPickupTarget }
        action { elbow rotate elbowForPickupTarget + 25 }
        action { extender goTo extenderToPickupTarget }
        action { elbow rotate elbowForPickupTarget }
        action { gripper goTo GRIPPER_OPEN }
        this += homeSequence
    }

    fun moveObjectToTarget() = sequence {
        name = ROTO_PICKUP
        this += pickupFromLocation
        action { waist rotate dropOffWaist }
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
        this += homeSequence
        action {
            waist rotate dropOffWaist
            gripper goTo GRIPPER_OPEN
        }
        action {
            extender goTo if (dropOffExtender >= 10) dropOffExtender - 10 else dropOffExtender
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

    companion object {
        const val MAGIC_EXTENDER = 15
        const val ROTO_PICKUP = "LocationPickup"
        const val ROTO_RETURN = "ReturnPickup"
    }
}
