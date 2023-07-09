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
import crackers.kobots.app.bus.*
import crackers.kobots.app.crickitHat
import crackers.kobots.app.executeWithMinTime
import crackers.kobots.app.executor
import crackers.kobots.app.parts.RotatorServo
import crackers.kobots.app.parts.RotatorStepper
import crackers.kobots.app.runFlag
import crackers.kobots.devices.at
import crackers.kobots.utilities.KobotSleep
import org.tinylog.Logger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private fun ActionSpeed.toMillis(): Long {
    return when (this) {
        ActionSpeed.SLOW -> 50
        ActionSpeed.NORMAL -> 15
        ActionSpeed.FAST -> 7
        else -> 15
    }
}

/**
 * V5 iteration of a controlled "arm-like" structure.
 */
object TheArm {
    const val STATE_TOPIC = "TheArm.State"
    const val REQUEST_TOPIC = "TheArm.Request"

    // build 2 gear ratio = 1.4:1
    private const val EXTENDER_DELTA = 1f
    const val EXTENDER_IN = 180f // straight up
    const val EXTENDER_OUT = 0f // all the way out

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

    private val HOME_POSITION = ArmPosition(
        JointPosition(WAIST_HOME),
        JointPosition(EXTENDER_IN),
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
            this at EXTENDER_IN
        }
        RotatorServo(servo3, EXTENDER_IN, EXTENDER_OUT, EXTENDER_DELTA)
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
        extender.rotate { angle = EXTENDER_IN }
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

    fun stop() {
        if (moveInProgress.get()) stopImmediately.set(true)
        while (stopImmediately.get()) KobotSleep.millis(5)
        waist.release()
    }

    // do stuff =======================================================================================================
    private val moveInProgress = AtomicBoolean(false)
    private val stopImmediately = AtomicBoolean(false)

    private fun canRun() = runFlag.get() && !stopImmediately.get()

    private fun handleRequest(request: KobotsAction) {
        when (request) {
            is EmergencyStop -> stopImmediately.set(true)
            is SequenceRequest -> executeSequence(request)
            else -> {}
        }
    }

    private fun executeSequence(request: SequenceRequest) {
        // claim it for ourselves and then use that for loop control
        if (!moveInProgress.compareAndSet(false, true)) return
        state = ArmState(state.position, true)
        atHome.set(false) // any request is treated as being "not home" but the GO_HOME will reset this

        executor.submit {
            try {
                request.sequence.build().forEach { action ->
                    if (!canRun()) return@forEach
                    var running = true
                    while (running) {
                        executeWithMinTime(action.speed.toMillis()) {
                            running = action.action.step()
                        }
                        updateCurrentState()
                    }
                }
            } catch (e: Exception) {
                Logger.error("Error executing sequence", e)
            }
            waist.release()

            // done
            moveInProgress.compareAndSet(true, false)
            updateCurrentState()
            stopImmediately.compareAndSet(true, false)
        }
    }


    /**
     * Updates the state of the arm, which in turn publishes to the event topic.
     */
    fun updateCurrentState() {
        state = ArmState(
            ArmPosition(
                JointPosition(waist.current()),
                JointPosition(extender.current()),
                JointPosition(gripper.current()),
                JointPosition(elbow.current())
            ),
            moveInProgress.get()
        )
    }
}
