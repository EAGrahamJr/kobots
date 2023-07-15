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
    const val ELBOW_UP = 90
    const val ELBOW_DOWN = -10

    private const val WAIST_DELTA = 1f
    const val WAIST_HOME = 0
    const val WAIST_MAX = 180f

    // not part of the arm, but used in coordination with it
    // TODO physical implementation left something to be desired
//    val mainRoto by lazy {
//        val stepper = BasicStepperMotor(2048, crickitHat.unipolarStepperPort())
//        RotatorStepper(stepper)
//    }
    val mainRoto = object : Rotator {
        private var angle = 0
        override fun rotateTo(angle: Int): Boolean {
            this.angle = angle
            return true
        }

        override fun current(): Int {
            return angle
        }
    }

    private val HOME_POSITION = ArmPosition(
        JointPosition(WAIST_HOME),
        JointPosition(0),
        JointPosition(0),
        JointPosition(ELBOW_UP)
    )

    // hardware! =====================================================================================================
    const val GRIPPER_OPEN = 0
    const val GRIPPER_CLOSED = 100

    val gripper by lazy {
        // traverse gears
        val _OPEN = 0f
        val _CLOSE = 65f

        val servo4 = crickitHat.servo(3, ServoTrim.TOWERPRO_SG90).apply {
            this at _CLOSE
        }

        // 0% == CLOSE, 100% == OPEN
        ServoLinearActuator(servo4, _OPEN, _CLOSE)
    }

    const val EXTENDER_HOME = 0
    const val EXTENDER_FULL = 100

    val extender by lazy {
        val _IN = 180f
        val _OUT = 0f
        val servo3 = crickitHat.servo(1, ServoTrim.TOWERPRO_SG90).apply {
            this at _IN
        }
        ServoLinearActuator(servo3, _IN, _OUT)
    }

    val elbow by lazy {
        val servo2 = crickitHat.servo(2, ServoTrim.TOWERPRO_SG90).apply {
            this at 0f
        }
        val physicalRange = IntRange(ELBOW_DOWN.toInt(), ELBOW_UP.toInt())
        val servoRange = IntRange(180, 0)
        RotatorServo(servo2, physicalRange, servoRange)
    }

    val waist by lazy {
        RotatorStepper(BasicStepperMotor(200, crickitHat.motorStepperPort()), 2.33f, true)
    }

    /**
     * Home the arm.
     */
    val homeAction = ActionBuilder().apply {
        waist rotate WAIST_HOME
        extender goTo EXTENDER_HOME
        elbow rotate ELBOW_UP
        gripper goTo GRIPPER_CLOSED
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
                JointPosition(extender.current()),
                JointPosition(gripper.current()),
                JointPosition(elbow.current())
            ),
            moveInProgress
        )
    }
}
