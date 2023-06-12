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
import crackers.kobots.app.*
import crackers.kobots.devices.at
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * --First-- Second attempt at a controlled "arm-like" structure.
 */
object TheArm {
    const val STATE_TOPIC = "TheArm.State"
    const val REQUEST_TOPIC = "TheArm.Request"

    // build 2 gear ratio = 2:1
    private const val SHOULDER_DELTA = 1f
    const val SHOULDER_UP = 180f // pulled in`
    const val SHOULDER_DOWN = 0f // close to horizontal

    // TODO do we have enough torque for another servo?
    private const val ELBOW_DELTA = 1f
    const val ELBOW_STRAIGHT = 0f
    const val ELBOW_BENT = 90f

    // traverse gears
    const val GRIPPER_OPEN = 60f
    const val GRIPPER_CLOSE = 9f

    private const val MAX_WAIST_STEPS = 200 * 1.31f // 36->20->->24->24->60S

    val HOME_POSITION = ArmPosition(
        JointPosition(0f), JointPosition(SHOULDER_UP), JointPosition(GRIPPER_CLOSE),
        JointPosition(ELBOW_STRAIGHT)
    )

    // hardware! =====================================================================================================
    private val waistStepper by lazy {
        val stepper = BasicStepperMotor(200, crickitHat.motorStepperPort())
        ArmStepper(stepper, MAX_WAIST_STEPS, false)
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
            this at ELBOW_STRAIGHT
        }
        ArmServo(servo2, ELBOW_STRAIGHT, ELBOW_BENT, ELBOW_DELTA)
    }

    val GO_HOME = ArmMovement(
        waist = JointMovement(HOME_POSITION.waist.angle),
        shoulder = JointMovement(HOME_POSITION.shoulder.angle),
        elbow = JointMovement(HOME_POSITION.elbow.angle),
        gripper = JointMovement(HOME_POSITION.gripper.angle)
    )


    // manage the state of this construct =============================================================================
    private val MOVE_TO_HOME = mapOf(waistStepper to 0f, shoulderServo to SHOULDER_UP, gripperServo to GRIPPER_CLOSE)

    private val _currentState = AtomicReference(ArmState(HOME_POSITION, false))
    var state: ArmState
        get() = _currentState.get()
        private set(s) {
            publishToTopic(STATE_TOPIC, s)
            _currentState.set(s)
        }

    fun start() {
        joinTopic(
            REQUEST_TOPIC,
            KobotsSubscriber<ArmRequest> {
                handleRequest(it)
            }
        )
    }

    fun stop() {
        moveInProgress.set(true)
        moveTo(MOVE_TO_HOME, Duration.ofMillis(50))
        waistStepper.release()
    }

    // do stuff =======================================================================================================
    private val moveInProgress = AtomicBoolean(false)

    private fun handleRequest(request: ArmRequest) {
        // claim it for ourselves and then use that for loop control
        if (!moveInProgress.compareAndSet(false, true)) return
        state = ArmState(state.position, true)

        executor.submit {
            request.movements.forEach { moveHere ->
                // TODO figure out interrupt mechanism?
                if (runFlag.get()) {
                    val moveThese = mutableMapOf<Rotatable, Float>()
                    if (moveHere.waist != NO_OP) moveThese[waistStepper] =
                        calculateMovement(moveHere.waist, waistStepper)
                    if (moveHere.shoulder != NO_OP) moveThese[shoulderServo] =
                        calculateMovement(moveHere.shoulder, shoulderServo)
                    if (moveHere.elbow != NO_OP) moveThese[elbowServo] = calculateMovement(moveHere.elbow, elbowServo)
                    if (moveHere.gripper != NO_OP) moveThese[gripperServo] =
                        calculateMovement(moveHere.gripper, gripperServo)
                    moveTo(moveThese, moveHere.stepPause)
                    // where everything is
                    state = ArmState(
                        ArmPosition(
                            JointPosition(waistStepper.current()),
                            JointPosition(shoulderServo.current()),
                            JointPosition(gripperServo.current()),
                            JointPosition(elbowServo.current())
                        ),
                        true
                    )
                }
            }
            // TODO this is temporary to figure out WTF is going on with the waist
            waistStepper.release()

            // done
            state = ArmState(
                ArmPosition(
                    JointPosition(waistStepper.current()),
                    JointPosition(shoulderServo.current()),
                    JointPosition(gripperServo.current()),
                    JointPosition(elbowServo.current())
                ),
                false
            )
            moveInProgress.compareAndSet(true, false)
        }
    }

    private fun calculateMovement(movement: JointMovement, device: Rotatable): Float =
        if (movement.relative) {
            device.current() + movement.angle
        } else {
            movement.angle
        }

    /**
     * Attempts to complete a full position change
     */
    private fun moveTo(moveThese: Map<Rotatable, Float>, stepPause: Duration) {
        var movementDone = false
        while (!movementDone && runFlag.get()) {
            executeWithMinTime(stepPause.toMillis()) {
                // are we at the desired state?
                movementDone = moveThese.map { e -> e.key.moveTowards(e.value) }.all { it }
            }
        }
    }
}
