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
import crackers.kobots.parts.ActionBuilder
import crackers.kobots.parts.ActionSequence

/**
 * "Second" demo: picks up a small bottle of eye drops and moves it to a new location.
 */
object EyeDropDemo {
    private const val LOAD_EXTENDER = 65
    private const val LOAD_GRIPPER = 90
    private const val ELBOW_FOR_DROPS = -5
    private const val TRAVEL_ELBOW = 60
    private const val TARGET_WAIST = 90
    private const val TARGET_EXTENDER = 30

    var hasPickedUpEyeDrops = false

    /**
     * Move to a more transportable position.
     */
    private val transportDropsPosition = ActionBuilder().apply {
        TheArm.elbow rotate TRAVEL_ELBOW
        TheArm.extender goTo TheArm.EXTENDER_HOME
    }

    /**
     * Has picked it up or put it down.
     */
    private fun ActionSequence.dropsDownOrUp(pickedUp: Boolean) {
        action { TheArm.gripper goTo TheArm.GRIPPER_OPEN }

        action {
            TheArm.elbow rotate {
                angle = TRAVEL_ELBOW
                stopCheck = {
                    hasPickedUpEyeDrops = pickedUp
                    false
                }
            }
        }
    }

    /**
     * Pick up something from the starting point and deliver it to the exit point.
     */
    val pickAndMove by lazy {
        crackers.kobots.parts.sequence {
            name = "Pick up drops"
            this + TheArm.homeAction

            action {
                TheArm.elbow rotate ELBOW_FOR_DROPS
                TheArm.gripper goTo TheArm.GRIPPER_OPEN
                TheArm.extender goTo LOAD_EXTENDER.retract()
            }
            action { TheArm.extender goTo LOAD_EXTENDER }
            action { TheArm.gripper goTo LOAD_GRIPPER }
            this + transportDropsPosition
            action { TheArm.waist rotate TARGET_WAIST }
            action {
                TheArm.extender goTo TARGET_EXTENDER
                TheArm.elbow rotate ELBOW_FOR_DROPS
            }
            dropsDownOrUp(true)
            this + TheArm.homeAction
        }
    }

    /**
     * Returns the thing from above.
     */
    val returnTheThing by lazy {
        crackers.kobots.parts.sequence {
            name = "Return the drops"
            this + TheArm.homeAction
            action { TheArm.waist rotate TARGET_WAIST }
            action {
                TheArm.elbow rotate ELBOW_FOR_DROPS
                TheArm.gripper goTo TheArm.GRIPPER_OPEN
                TheArm.extender goTo TARGET_EXTENDER.retract()
            }
            action { TheArm.extender goTo TARGET_EXTENDER }
            action { TheArm.gripper goTo LOAD_GRIPPER }
            this + transportDropsPosition
            action { TheArm.waist rotate TheArm.WAIST_HOME }
            action {
                TheArm.elbow rotate ELBOW_FOR_DROPS
                TheArm.extender goTo LOAD_EXTENDER
            }
            dropsDownOrUp(false)
            this + TheArm.homeAction
        }
    }
}

/**
 * Effectively a re-write of the above, but using the abstract base class.
 */
object OtherDropDemo : PickAndMove() {
    override val pickupSequenceName = "P2"
    override val pickupElbow: Int = -5
    override val pickupExtender: Int = 65
    override val pickupWaist: Int = 0

    override val gripperGrab: Int = 90

    override val transportElbow: Int = 60

    override val targetSequenceName = "T2"
    override val targetElbow: Int = pickupElbow
    override val targetExtender: Int = 30
    override val targetWaist: Int = 90
}
