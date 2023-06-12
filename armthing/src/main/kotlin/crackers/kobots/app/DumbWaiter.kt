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

package crackers.kobots.app

import crackers.kobots.app.arm.ArmMovement
import crackers.kobots.app.arm.ArmRequest
import crackers.kobots.app.arm.JointMovement
import crackers.kobots.app.arm.TheArm
import crackers.kobots.app.arm.TheArm.ELBOW_STRAIGHT
import crackers.kobots.app.arm.TheArm.GO_HOME
import crackers.kobots.app.arm.TheArm.GRIPPER_OPEN
import crackers.kobots.app.arm.TheArm.SHOULDER_UP
import java.time.Duration
import kotlin.system.exitProcess

private val buttons by lazy { (1..4).toList().map { crickitHat.touchDigitalIn(it) } }

private val NO_BUTTONS = listOf(false, false, false, false)
private var lastButtonValues = NO_BUTTONS
private lateinit var currentButtons: List<Boolean>

// because we're looking for "presses", only return values when a value transitions _to_ true
private fun buttonCheck(): Boolean {
    currentButtons = buttons.map { it.value }.let { read ->
        // TODO add an elapsed time check?

        if (read == lastButtonValues) {
            NO_BUTTONS
        } else {
            lastButtonValues = read
            read
        }
    }
    return currentButtons.isEmpty() || !currentButtons[3]
}

private const val WAIT_LOOP = 100L

// TODO temporary while testing
const val REMOTE_PI = "diozero.remote.hostname"
const val MARVIN = "marvin.local"

/**
 * Run this.
 */
fun main() {
//    System.setProperty(REMOTE_PI, MARVIN)

    crickitHat.use { hat ->
        TheArm.start()


        val waistDance = ArmRequest(
            listOf(
                ArmMovement(waist = JointMovement(90f), stepPause = Duration.ofMillis(25)),
                GO_HOME
            )
        )
        val elbowDance = ArmRequest(
            listOf(
                ArmMovement(elbow = JointMovement(15f)), GO_HOME
            )
        )


        // main loop!!!!!
        while (buttonCheck()) {
            executeWithMinTime(WAIT_LOOP) {
                // figure out if we're doing anything
                if (!TheArm.state.busy && currentButtons[0]) publishToTopic(TheArm.REQUEST_TOPIC, tireDance)
                if (!TheArm.state.busy && currentButtons[1]) publishToTopic(TheArm.REQUEST_TOPIC, elbowDance)
            }
        }
        runFlag.set(false)
        TheArm.stop()
    }
    executor.shutdownNow()
    exitProcess(0)
}

val tireDance by lazy {
    val ELB_MOVE = 30f
    val SH_DOWN = 10f
    val SH_MID = 90f
    val GR_GRAB = 20f
    val WST_HALF = 45f
    val WST_ALL = 90f
    ArmRequest(
        listOf(
            GO_HOME,
            // open first
            ArmMovement(gripper = JointMovement(GRIPPER_OPEN), stepPause = Duration.ZERO),
            // down
            ArmMovement(shoulder = JointMovement(SH_DOWN), elbow = JointMovement(ELB_MOVE)),
            // grab
            ArmMovement(gripper = JointMovement(GR_GRAB), stepPause = Duration.ZERO),
            // up a bit
            ArmMovement(shoulder = JointMovement(SH_MID)),
            // 1/2 way
            ArmMovement(
                waist = JointMovement(WST_HALF),
                shoulder = JointMovement(SHOULDER_UP),
                elbow = JointMovement(ELBOW_STRAIGHT)
            ),
            ArmMovement(
                waist = JointMovement(WST_ALL),
                shoulder = JointMovement(SH_MID),
                elbow = JointMovement(ELB_MOVE)
            ),
            // put it down
            ArmMovement(shoulder = JointMovement(SH_DOWN)),
            ArmMovement(gripper = JointMovement(GRIPPER_OPEN), stepPause = Duration.ZERO),
            // clear
            ArmMovement(shoulder = JointMovement(SH_MID)),
            // home
            GO_HOME
        )
    )
}
