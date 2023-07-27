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
import crackers.kobots.parts.ActionBuilder
import crackers.kobots.parts.ActionSequence
import crackers.kobots.parts.sequence

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

    class MovePosition(val name: String, val waist: Int, val elbow: Int, val extender: Int)

    private val pickupPosition by lazy {
        MovePosition(pickupSequenceName, pickupWaist, pickupElbow, pickupExtender)
    }
    private val targetPosition: MovePosition by lazy {
        MovePosition(targetSequenceName, targetWaist, targetElbow, targetExtender)
    }

    private val getOutOfTheWay by lazy {
        ActionBuilder().apply {
            elbow rotate transportElbow
            extender goTo EXTENDER_HOME
        }
    }

    private fun pickUpItem() = sequence {
        // pick it up and get back
        action { gripper goTo gripperGrab }
        plus(getOutOfTheWay)
    }

    private fun putDownItem() = sequence {
        // open the gripper and get out of the way
        action { gripper goTo GRIPPER_OPEN }
        this + getOutOfTheWay
    }

    fun pickupItem(): ActionSequence = moveAThing(pickupPosition, targetPosition)

    fun returnItem(): ActionSequence = moveAThing(targetPosition, pickupPosition)

    private fun moveAThing(startPosition: MovePosition, endPosition: MovePosition) = sequence {
        name = startPosition.name
        this + getOutOfTheWay

        action {
            waist rotate startPosition.waist
            gripper goTo GRIPPER_OPEN
        }
        action {
            elbow rotate startPosition.elbow
            extender goTo startPosition.extender / 2
        }
        action { extender goTo startPosition.extender }

        this += pickUpItem()

        action { waist rotate endPosition.waist }
        action {
            extender goTo endPosition.extender
            elbow rotate endPosition.elbow / 2
        }
        action { elbow rotate endPosition.elbow }

        this += putDownItem()

        this + getOutOfTheWay
    }
}
