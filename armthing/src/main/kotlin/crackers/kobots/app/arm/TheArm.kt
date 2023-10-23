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

import com.diozero.api.ServoTrim
import crackers.kobots.app.AppCommon
import crackers.kobots.app.AppCommon.SLEEP_TOPIC
import crackers.kobots.app.AppCommon.runFlag
import crackers.kobots.app.crickitHat
import crackers.kobots.app.execution.armSleep
import crackers.kobots.app.execution.homeSequence
import crackers.kobots.devices.at
import crackers.kobots.parts.app.KobotsAction
import crackers.kobots.parts.app.KobotsSubscriber
import crackers.kobots.parts.app.joinTopic
import crackers.kobots.parts.app.publishToTopic
import crackers.kobots.parts.movement.*
import java.util.concurrent.atomic.AtomicReference

/**
 * V5 iteration of a controlled "arm-like" structure.
 */
object TheArm : SequenceExecutor("TheArm", AppCommon.mqttClient) {
    const val STATE_TOPIC = "TheArm.State"
    const val REQUEST_TOPIC = "TheArm.Request"

    fun request(sequence: ActionSequence) {
        publishToTopic(REQUEST_TOPIC, SequenceRequest(sequence))
    }

    const val ELBOW_UP = 90
    const val ELBOW_DOWN = -10

    const val WAIST_HOME = 0
    const val WAIST_MAX = 110

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

        val servo = crickitHat.servo(3, ServoTrim.TOWERPRO_SG90).apply {
            this at _CLOSE
        }

        // 0% == CLOSE, 100% == OPEN
        ServoLinearActuator(servo, _OPEN, _CLOSE)
    }

    val extender by lazy {
        val _IN = 180f
        val _OUT = 0f
        val servo = crickitHat.servo(1, ServoTrim.TOWERPRO_SG90).apply {
            this at _IN
        }
        ServoLinearActuator(servo, _IN, _OUT)
    }

    val elbow by lazy {
        val servo2 = crickitHat.servo(2, ServoTrim.TOWERPRO_SG90).apply {
            this at 0
        }
        val physicalRange = IntRange(ELBOW_DOWN, ELBOW_UP)
        val servoRange = IntRange(180, 0)
        ServoRotator(servo2, physicalRange, servoRange)
    }

    val waist by lazy {
        val _HOME = 0
        val _MAX = 180

        val servoRange = IntRange(_HOME, _MAX)
        val physicalRange = IntRange(0, 140) // MG90S servo trim
        val servo = crickitHat.servo(4, ServoTrim(1500, 1100)).apply {
            this at _HOME
        }
        ServoRotator(servo, physicalRange, servoRange)
    }

    private val noodle by lazy { crickitHat.signalDigitalOut(8) }
    private var noodValue = false
    var nood: Boolean
        get() = noodValue
        set(value) {
            noodValue = value
            noodle.setValue(value)
        }

    // manage the state of this construct =============================================================================
    private val _currentState = AtomicReference(ArmState(HOME_POSITION, false))

    var state: ArmState
        get() = _currentState.get()
        private set(s) {
            _currentState.set(s)
            publishToTopic(STATE_TOPIC, s)
        }
    val home: Boolean
        get() = state.position == HOME_POSITION

    fun start() {
        joinTopic(
            REQUEST_TOPIC,
            KobotsSubscriber<KobotsAction> {
                handleRequest(it)
            }
        )
        joinTopic(
            SLEEP_TOPIC,
            KobotsSubscriber { event ->
                if (event is AppCommon.SleepEvent && event.sleep) handleRequest(SequenceRequest((armSleep)))
            }
        )
    }

    // allow the home sequence to run as well if termination has been called
    override fun canRun() = runFlag.get() || currentSequence.get() == homeSequence.name

    override fun stop() {
        super.stop()
        postExecution()
    }

    override fun preExecution() {
        state = ArmState(state.position, true)
        nood = true
    }

    override fun postExecution() {
        nood = false
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
