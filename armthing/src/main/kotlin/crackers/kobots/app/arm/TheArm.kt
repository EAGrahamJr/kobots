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
import com.diozero.devices.sandpit.motor.BasicStepperMotor
import crackers.kobots.app.SequenceExecutor
import crackers.kobots.app.bus.KobotsAction
import crackers.kobots.app.bus.KobotsSubscriber
import crackers.kobots.app.bus.joinTopic
import crackers.kobots.app.bus.publishToTopic
import crackers.kobots.app.crickitHat
import crackers.kobots.devices.at
import crackers.kobots.parts.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * V5 iteration of a controlled "arm-like" structure.
 */
object TheArm : SequenceExecutor() {
    const val STATE_TOPIC = "TheArm.State"
    const val REQUEST_TOPIC = "TheArm.Request"

    private const val ELBOW_DELTA = 1f
    const val ELBOW_UP = 0f
    const val ELBOW_DOWN = 180f

    // traverse gears
    const val GRIPPER_OPEN = 0f
    const val GRIPPER_CLOSE = 65f
//    val GRIPPER_HOME = JointMovement(GRIPPER_CLOSE)

    private const val WAIST_DELTA = 1f
    const val WAIST_HOME = 0f
    const val WAIST_MAX = 180f

    // not part of the arm, but used in coordination with it
    // TODO physical implementation left something to be desired
//    val mainRoto by lazy {
//        val stepper = BasicStepperMotor(2048, crickitHat.unipolarStepperPort())
//        RotatorStepper(stepper)
//    }
    val mainRoto = object : Rotator {
        private var angle = 0f
        override fun rotateTo(angle: Float): Boolean {
            this.angle = angle
            return true
        }

        override fun current(): Float {
            return angle
        }
    }

    private val HOME_POSITION = ArmPosition(
        JointPosition(WAIST_HOME),
        JointPosition(0f),
        JointPosition(GRIPPER_CLOSE),
        JointPosition(ELBOW_UP)
    )

    // hardware! =====================================================================================================
    val gripper by lazy {
        val servo4 = crickitHat.servo(3, ServoTrim.TOWERPRO_SG90).apply {
            this at GRIPPER_CLOSE
        }

        fun open(): Boolean {
            servo4 at GRIPPER_OPEN
            return true
        }
        RotatorServo(servo4, GRIPPER_CLOSE, GRIPPER_OPEN)
    }

    val extender by lazy {
        val servo3 = crickitHat.servo(1, ServoTrim.TOWERPRO_SG90).apply {
            this at 180f
        }
        ServoLinearActuator(servo3, 180f, 0f)
    }

    val elbow by lazy {
        val servo2 = crickitHat.servo(2, ServoTrim.TOWERPRO_SG90).apply {
            this at ELBOW_UP
        }
        RotatorServo(servo2, ELBOW_UP, ELBOW_DOWN, ELBOW_DELTA)
    }

    val waist by lazy {
        RotatorStepper(BasicStepperMotor(200, crickitHat.motorStepperPort()), 2.33f, true)
    }

    /**
     * Home the arm.
     */
    val homeAction = ActionBuilder().apply {
        waist.rotate { angle = WAIST_HOME }
        extender.extend { distance = 0 }
        elbow.rotate { angle = ELBOW_UP }
        gripper.rotate { angle = GRIPPER_CLOSE }
    }

    // manage the state of this construct =============================================================================
    private val _currentState = AtomicReference(ArmState(HOME_POSITION, false))
    private val atHome = AtomicBoolean(true)

    var state: ArmState
        get() = _currentState.get()
        private set(s) {
            _currentState.set(s)
            publishToTopic(STATE_TOPIC, s)
            atHome.set(s.position == HOME_POSITION)
        }

    fun start() {
        joinTopic(
            REQUEST_TOPIC,
            KobotsSubscriber<KobotsAction> {
                handleRequest(it)
            }
        )
    }

    override fun stop() {
        super.stop()
        postExecution()
    }

    override fun preExecution() {
        state = ArmState(state.position, true)
        atHome.set(false) // any request is treated as being "not home" but the GO_HOME will reset this
    }

    override fun postExecution() {
        waist.release()
    }

    /**
     * Updates the state of the arm, which in turn publishes to the event topic.
     */
    override fun updateCurrentState() {
        state = ArmState(
            ArmPosition(
                JointPosition(waist.current()),
                JointPosition(extender.current().toFloat()),
                JointPosition(gripper.current()),
                JointPosition(elbow.current())
            ),
            moveInProgress
        )
    }
}
