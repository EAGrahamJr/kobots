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
import crackers.kobots.app.enviro.DieAufseherin.DA_TOPIC
import crackers.kobots.app.enviro.DieAufseherin.dropOffComplete
import crackers.kobots.app.enviro.DieAufseherin.returnComplete
import crackers.kobots.app.homeSequence
import crackers.kobots.execution.publishToTopic
import crackers.kobots.parts.sequence

/**
 * Picks up stuff from the Rotomatic and returns it.
 */
object PickWithRotomatic {
    private val defaultMover = MoveStuffFromRotomatic(elbowForRotomatic = 11)
    val moveStandingObjectToTarget = defaultMover.moveObjectToTarget()
    val standingPickupAndReturn = defaultMover.pickupAndReturn()

    private val defaultThinItemMover = MoveStuffFromRotomatic(
        elbowForRotomatic = 9,
        closeOnItem = GRIPPER_CLOSED,
        dropOffElbow = ELBOW_DOWN
    )
    val moveThinItemToTarget = defaultThinItemMover.moveObjectToTarget()
    val thinItemReturn = defaultThinItemMover.pickupAndReturn()
}

/**
 * Magic numbers for the Rotomatic
 */
const val MAGIC_EXTENDER = 15

class MoveStuffFromRotomatic(
    val closeOnItem: Int = 90, // how much to close the gripper to grab the item - this stresses the technic a bit

    val extenderToRotomatic: Int = 66, // how far to extend the extender to reach the Rotomatic
    val elbowForRotomatic: Int = 10, // how far to rotate the elbow to reach the Rotomatic

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

    private val pickupFromRoto = sequence {
        name = "Pick Up From Rotomatic"
        this += backOffAndHome

        // avoid collisions!!
        action {
            waist rotate WAIST_HOME
            gripper goTo GRIPPER_OPEN
        }
        action { elbow rotate elbowForRotomatic }
        action { extender goTo extenderToRotomatic }

        action { gripper goTo closeOnItem }

        // basically just pick it up a little bit
        action {
            elbow rotate elbowForRotomatic + 5
            extender goTo MAGIC_EXTENDER
        }

        // reel it in!!!!
        action {
            extender goTo EXTENDER_HOME
            elbow rotate elbowForTransport
        }
    }

    // assumes something is in the gripper
    private val returnToRotomatic = sequence {
        name = "Return To Rotomatic"
        action { waist rotate WAIST_HOME }
        action { elbow rotate elbowForRotomatic + 3 }
        action { extender goTo extenderToRotomatic }
        action { elbow rotate elbowForRotomatic }
        action { gripper goTo GRIPPER_OPEN }
        this += backOffAndHome
    }

    fun moveObjectToTarget() = sequence {
        this += pickupFromRoto
        action { waist rotate 90 }
        action { extender goTo dropOffExtender }
        action { elbow rotate dropOffElbow }
        action { gripper goTo GRIPPER_OPEN }
        action {
            extender goTo EXTENDER_HOME
            elbow rotate elbowForTransport
        }
        this += homeSequence
        // signal completion of this specific thing
        action {
            execute {
                publishToTopic(DA_TOPIC, dropOffComplete)
                true
            }
        }
    }

    fun pickupAndReturn() = sequence {
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
        this += returnToRotomatic
        // signal completion of this specific thing
        action {
            execute {
                publishToTopic(DA_TOPIC, returnComplete)
                true
            }
        }
    }
}
