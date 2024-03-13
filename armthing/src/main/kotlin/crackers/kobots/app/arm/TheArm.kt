/*
 * Copyright 2022-2024 by E. A. Graham, Jr.
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

import crackers.kobots.app.AppCommon
import crackers.kobots.app.AppCommon.runFlag
import crackers.kobots.app.Startable
import crackers.kobots.app.crickitHat
import crackers.kobots.app.enviro.DieAufseherin
import crackers.kobots.app.enviro.HAStuff
import crackers.kobots.app.execution.homeSequence
import crackers.kobots.devices.MG90S_TRIM
import crackers.kobots.devices.at
import crackers.kobots.parts.app.publishToTopic
import crackers.kobots.parts.movement.*
import java.util.concurrent.atomic.AtomicReference

/**
 * V5 iteration of a controlled "arm-like" structure.
 */
object TheArm : SequenceExecutor("TheArm", AppCommon.mqttClient), Startable {
    const val REQUEST_TOPIC = "TheArm.Request"

    fun request(sequence: ActionSequence) {
        publishToTopic(REQUEST_TOPIC, SequenceRequest(sequence))
    }

    const val ELBOW_UP = 90
    const val ELBOW_DOWN = -9

    const val WAIST_HOME = 0
    const val WAIST_MAX = 140

    const val EXTENDER_HOME = 0
    const val EXTENDER_FULL = 100

    const val GRIPPER_OPEN = 0
    const val GRIPPER_CLOSED = 100

    private val HOME_POSITION = ArmPosition(
        JointPosition(WAIST_HOME),
        JointPosition(EXTENDER_HOME),
        JointPosition(GRIPPER_CLOSED),
        JointPosition(ELBOW_UP)
    )

    // hardware! =====================================================================================================

    val gripper by lazy {
        // traverse gears
        val _OPEN = 0f
        val _CLOSE = 65f

        val servo = crickitHat.servo(3, MG90S_TRIM).apply {
            this at _CLOSE
        }

        // 0% == CLOSE, 100% == OPEN
        ServoLinearActuator(servo, _OPEN, _CLOSE)
    }

    val extender by lazy {
        val _IN = 0f
        val _OUT = 90f
        val servo = crickitHat.servo(1, MG90S_TRIM).apply {
            this at _IN
        }
        ServoLinearActuator(servo, _IN, _OUT)
    }

    val elbow by lazy {
        val servo2 = crickitHat.servo(2, MG90S_TRIM).apply {
            this at 0
        }
        val physicalRange = IntRange(ELBOW_DOWN, ELBOW_UP)
        val servoRange = IntRange(150, 0)
        ServoRotator(servo2, physicalRange, servoRange)
    }

    val waistServo by lazy {
        crickitHat.servo(4, MG90S_TRIM).apply { this at 0 }
    }

    val waist by lazy {
        val servoRange = IntRange(0, 180)
        val physicalRange = IntRange(-45, 200)    // TODO check this
        ServoRotator(waistServo, physicalRange, servoRange)
    }


    // manage the state of this construct =============================================================================
    private val _currentState = AtomicReference(ArmState(HOME_POSITION, false))

    var state: ArmState
        get() = _currentState.get()
        private set(s) {
            _currentState.set(s)
        }
    val home: Boolean
        get() = state.position == HOME_POSITION

    override fun start() {
        // TODO do we need the interna bus?
//        joinTopic(REQUEST_TOPIC, KobotsSubscriber<KobotsAction> { handleRequest(it) })
    }

    // allow the home sequence to run as well if termination has been called
    override fun canRun() = runFlag.get() || currentSequence.get() == homeSequence.name

    override fun stop() {
        super.stop()
        postExecution()
    }

    override fun preExecution() {
        state = ArmState(state.position, true)
        HAStuff.noodSwitch.handleCommand("ON")
        DieAufseherin.currentMode = DieAufseherin.SystemMode.IN_MOTION
    }

    override fun postExecution() {
        HAStuff.noodSwitch.handleCommand("OFF")
        HAStuff.numberWaistEntity.sendCurrentState()
        // just in case, release steppers
        DieAufseherin.currentMode = DieAufseherin.SystemMode.IDLE
    }

    /**
     * Updates the state of the arm, which in turn publishes to the event topic.
     */
    override fun updateCurrentState() {
        state = ArmState(
            ArmPosition(
                JointPosition(waist.current()),
                JointPosition(extender.current()),
                JointPosition(gripper.current()),
                JointPosition(elbow.current())
            ),
            moveInProgress
        )
    }
}
