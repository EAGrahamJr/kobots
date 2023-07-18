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

/**
 * Abstract class that will use two defined locations: a pickup location and a target location. There are two sequences
 * defined: [pickupItem] and [returnItem], which should be evident in their names.
 */
abstract class PickAndMove {
    protected abstract val pickupSequenceName: String
    protected abstract val pickupElbow: Int
    protected abstract val pickupExtender: Int
    protected abstract val pickupWaist: Int

    protected abstract val gripperGrab: Int

    protected abstract val transportElbow: Int

    protected abstract val targetSequenceName: String
    protected abstract val targetElbow: Int
    protected abstract val targetExtender: Int
    protected abstract val targetWaist: Int

    private fun pickUpItem() = crackers.kobots.parts.sequence {
        // pick it up
        action { TheArm.gripper goTo gripperGrab }
        action { TheArm.elbow rotate transportElbow }
        // retract the extender to home to avoid hitting anything
        action { TheArm.extender goTo TheArm.EXTENDER_HOME }
    }

    private fun putDownItem() = crackers.kobots.parts.sequence {
        // open the gripper
        action { TheArm.gripper goTo TheArm.GRIPPER_OPEN }
        // get out of the way
        action {
            TheArm.elbow rotate transportElbow
            TheArm.extender goTo TheArm.EXTENDER_HOME
        }
    }

    fun pickupItem() = crackers.kobots.parts.sequence {
        name = pickupSequenceName
        this + TheArm.homeAction

        // rotate to the pickup waist position and open the gripper
        action {
            TheArm.waist rotate pickupWaist
            TheArm.gripper goTo TheArm.GRIPPER_OPEN
        }
        // move into pickup position
        action {
            TheArm.extender goTo pickupExtender.retract()
            TheArm.elbow rotate pickupElbow
        }
        action { TheArm.extender goTo pickupExtender }
        this += pickUpItem()

        // turn the waist to the target position
        action { TheArm.waist rotate targetWaist }
        // move to the target position
        action { TheArm.extender goTo targetExtender }
        action { TheArm.elbow rotate targetElbow }

        this += putDownItem()

        this + TheArm.homeAction
    }

    fun returnItem() = crackers.kobots.parts.sequence {
        name = targetSequenceName
        this + TheArm.homeAction

        action {
            TheArm.waist rotate targetWaist
            TheArm.gripper goTo TheArm.GRIPPER_OPEN
        }
        action {
            TheArm.extender goTo targetExtender.retract()
            TheArm.elbow rotate targetElbow
        }
        action { TheArm.extender goTo targetExtender }

        this += pickUpItem()

        action { TheArm.waist rotate pickupWaist }
        action { TheArm.extender goTo pickupExtender }
        action { TheArm.elbow rotate pickupElbow }

        this += putDownItem()

        this + TheArm.homeAction
    }
}
