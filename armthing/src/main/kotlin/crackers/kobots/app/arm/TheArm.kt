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
import crackers.kobots.app.*
import crackers.kobots.devices.at
import crackers.kobots.utilities.KobotSleep
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * --First-- --Second-- Third attempt at a controlled "arm-like" structure.
 *
 * The "lowest" point that should be used is 45 on the shoulder and 45 on the elbow. If the elbow is > 45, it will
 * try to life the arm off the table.
 */
object TheArm {
    const val STATE_TOPIC = "TheArm.State"
    const val REQUEST_TOPIC = "TheArm.Request"

    // build 2 gear ratio = 1.4:1
    private const val SHOULDER_DELTA = 1f
    const val SHOULDER_UP = 180f // pulled in`
    const val SHOULDER_DOWN = 35f // close to horizontal

    private const val ELBOW_DELTA = 1f
    const val ELBOW_UP = 0f
    const val ELBOW_HOME = 90f
    const val ELBOW_BENT = 180f

    // traverse gears
    const val GRIPPER_OPEN = 70f
    const val GRIPPER_CLOSE = 9f
    val GRIPPER_HOME = JointMovement(GRIPPER_CLOSE)

    private const val WAIST_DELTA = 1f
    const val WAIST_HOME = 0f
    const val WAIST_MAX = 180f

    private val HOME_POSITION = ArmPosition(
        JointPosition(WAIST_HOME), JointPosition(SHOULDER_UP), JointPosition(GRIPPER_CLOSE),
        JointPosition(ELBOW_HOME)
    )

    // hardware! =====================================================================================================
    private val waistServo by lazy {
        val servo1 = crickitHat.servo(1, ServoTrim.TOWERPRO_SG90).apply {
            this at WAIST_HOME
        }
        ArmServo(servo1, WAIST_HOME, WAIST_MAX, WAIST_DELTA)
    }

    private val gripperServo by lazy {
        val servo4 = crickitHat.servo(4, ServoTrim.TOWERPRO_SG90).apply {
            this at GRIPPER_CLOSE
        }
        ArmServo(servo4, GRIPPER_CLOSE, GRIPPER_OPEN)
    }

    private val shoulderServo by lazy {
        val servo3 = crickitHat.servo(3, ServoTrim.TOWERPRO_SG90).apply {
            this at SHOULDER_UP
        }
        ArmServo(servo3, SHOULDER_UP, SHOULDER_DOWN, SHOULDER_DELTA)
    }

    private val elbowServo by lazy {
        val servo2 = crickitHat.servo(2, ServoTrim.TOWERPRO_SG90).apply {
            this at ELBOW_HOME
        }
        ArmServo(servo2, ELBOW_UP, ELBOW_BENT, ELBOW_DELTA)
    }

    val GO_HOME = ArmMovement(
        waist = JointMovement(HOME_POSITION.waist.angle),
        shoulder = JointMovement(HOME_POSITION.shoulder.angle),
        elbow = JointMovement(HOME_POSITION.elbow.angle),
        gripper = GRIPPER_HOME
    )


    // manage the state of this construct =============================================================================
    private val _currentState = AtomicReference(ArmState(HOME_POSITION, false))
    private val atHome = AtomicBoolean(true)

    var state: ArmState
        get() = _currentState.get()
        private set(s) {
            publishToTopic(STATE_TOPIC, s)
            _currentState.set(s)
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
    }

    // do stuff =======================================================================================================
    private val moveInProgress = AtomicBoolean(false)
    private val stopImmediately = AtomicBoolean(false)

    private fun canRun() = runFlag.get() && !stopImmediately.get()

    private fun handleRequest(request: KobotsAction) {
        when (request) {
            is EmergencyStop -> if (moveInProgress.get()) stopImmediately.set(true)
            is ArmSequence -> {
                executeSequence(request)
            }
            else -> {}
        }
    }

    private fun executeSequence(request: ArmSequence) {
        // claim it for ourselves and then use that for loop control
        if (!moveInProgress.compareAndSet(false, true)) return
        state = ArmState(state.position, true)
        atHome.set(false)   // any request is treated as being "not home" but the GO_HOME will reset this

        executor.submit {
            request.movements.forEach { moveHere ->
                // application still running and the interrupt isn't set
                if (canRun()) {
                    val moveThese = mutableMapOf<Rotatable, JointMovement>()
                    moveThese[waistServo] = calculateMovement(moveHere.waist, waistServo)
                    moveThese[shoulderServo] = calculateMovement(moveHere.shoulder, shoulderServo)
                    moveThese[elbowServo] = calculateMovement(moveHere.elbow, elbowServo)
                    moveThese[gripperServo] = calculateMovement(moveHere.gripper, gripperServo)
                    moveTo(moveThese, moveHere.stepPause)
                }
            }

            // done
            state = ArmState(
                ArmPosition(
                    JointPosition(waistServo.current()),
                    JointPosition(shoulderServo.current()),
                    JointPosition(gripperServo.current()),
                    JointPosition(elbowServo.current())
                ),
                false
            )
            moveInProgress.compareAndSet(true, false)
            stopImmediately.compareAndSet(true, false)
        }
    }

    private fun calculateMovement(movement: JointMovement, device: Rotatable): JointMovement =
        if (movement.relative) {
            JointMovement(device.current() + movement.angle, false, movement.stopCheck)
        } else {
            movement
        }

    /**
     * Attempts to complete a full position change
     */
    private fun moveTo(moveThese: Map<Rotatable, JointMovement>, stepPause: Duration) {
        var movementDone = false
        while (!movementDone && canRun()) {
            executeWithMinTime(stepPause.toMillis()) {
                // are we at the desired state?
                movementDone = moveThese
                    .map { e ->
                        val rotator = e.key
                        val movement = e.value
                        val stopCheck = movement.stopCheck()
                        stopCheck || rotator.moveTowards(movement.angle)
                    }.all { it }
            }
            // where everything is
            state = ArmState(
                ArmPosition(
                    JointPosition(waistServo.current()),
                    JointPosition(shoulderServo.current()),
                    JointPosition(gripperServo.current()),
                    JointPosition(elbowServo.current())
                ),
                true
            )
        }
    }
}
