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
import crackers.kobots.app.parts.Rotatable
import crackers.kobots.app.parts.RotatableServo
import crackers.kobots.app.parts.RotatableStepper
import crackers.kobots.devices.at
import crackers.kobots.utilities.KobotSleep
import org.tinylog.Logger
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

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
    val GRIPPER_HOME = JointMovement(GRIPPER_CLOSE)

    private const val WAIST_DELTA = 1f
    const val WAIST_HOME = 0f
    const val WAIST_MAX = 180f

    private val HOME_POSITION = ArmPosition(
        JointPosition(WAIST_HOME), JointPosition(EXTENDER_IN), JointPosition(GRIPPER_CLOSE),
        JointPosition(ELBOW_UP)
    )

    // hardware! =====================================================================================================
    private val gripper by lazy {
        val servo4 = crickitHat.servo(3, ServoTrim.TOWERPRO_SG90).apply {
            this at GRIPPER_CLOSE
        }
        RotatableServo(servo4, GRIPPER_CLOSE, GRIPPER_OPEN)
    }

    private val extender by lazy {
        val servo3 = crickitHat.servo(1, ServoTrim.TOWERPRO_SG90).apply {
            this at EXTENDER_IN
        }
        RotatableServo(servo3, EXTENDER_IN, EXTENDER_OUT, EXTENDER_DELTA)
    }

    private val elbow by lazy {
        val servo2 = crickitHat.servo(2, ServoTrim.TOWERPRO_SG90).apply {
            this at ELBOW_UP
        }
        RotatableServo(servo2, ELBOW_UP, ELBOW_DOWN, ELBOW_DELTA)
    }

    private val waist by lazy {
        RotatableStepper(BasicStepperMotor(200, crickitHat.motorStepperPort()), 2.33f, true)
    }

    val GO_HOME = ArmMovement(
        waist = JointMovement(HOME_POSITION.waist.angle),
        extender = JointMovement(HOME_POSITION.extender.angle),
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
        waist.release()
    }

    // do stuff =======================================================================================================
    private val moveInProgress = AtomicBoolean(false)
    private val stopImmediately = AtomicBoolean(false)

    private fun canRun() = runFlag.get() && !stopImmediately.get()

    private fun handleRequest(request: KobotsAction) {
        when (request) {
            is EmergencyStop -> stopImmediately.set(true)
            is ArmSequence -> {
                executeSequence(request)
            }

            is ManualMode -> manualMode(request.direction)

            else -> {}
        }
    }

    private val manualJoints = listOf(waist, extender, elbow, gripper)
    private var manualJointIndex = 0

    /**
     * "Manual" mode is a single joint movement in a single direction. `null` indicates "switch" to another joint.
     */
    private fun manualMode(direction: Boolean?) {
        try {
            when (direction) {
                null -> {
                    manualJointIndex = (manualJointIndex + 1) % manualJoints.size
                    publishToTopic(STATE_TOPIC, ManualModeEvent(manualJointIndex))
                }

                true -> {
                    val rotatable = manualJoints[manualJointIndex]
                    rotatable.moveTowards(rotatable.current() + 1f)
                }

                false -> {
                    val rotatable = manualJoints[manualJointIndex]
                    rotatable.moveTowards(rotatable.current() - 1f)
                }
            }
            state = ArmState(
                ArmPosition(
                    JointPosition(waist.current()),
                    JointPosition(extender.current()),
                    JointPosition(gripper.current()),
                    JointPosition(elbow.current())
                ),
                false
            )
        } catch (e: Exception) {
            Logger.error("ManualMode", e)
        }
    }

    private fun executeSequence(request: ArmSequence) {
        // claim it for ourselves and then use that for loop control
        if (!moveInProgress.compareAndSet(false, true)) return
        state = ArmState(state.position, true)
        atHome.set(false)   // any request is treated as being "not home" but the GO_HOME will reset this

        executor.submit {
            request.movements.forEach { moveHere ->
                try {
                    // application still running and the interrupt isn't set
                    if (canRun()) {
                        val moveThese = mutableMapOf<Rotatable, JointMovement>()
                        moveThese[waist] = calculateMovement(moveHere.waist, waist)
                        moveThese[extender] = calculateMovement(moveHere.extender, extender)
                        moveThese[elbow] = calculateMovement(moveHere.elbow, elbow)
                        moveThese[gripper] = calculateMovement(moveHere.gripper, gripper)
                        moveTo(moveThese, moveHere.stepPause)
                    }
                } catch (e: Exception) {
                    Logger.error("Error moving arm", e)
                }
                // release the stepper temporarily for heat dissipation
                waist.release()
            }

            // done
            state = ArmState(
                ArmPosition(
                    JointPosition(waist.current()),
                    JointPosition(extender.current()),
                    JointPosition(gripper.current()),
                    JointPosition(elbow.current())
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
                        with(e.value) {
                            stopCheck() || rotator.moveTowards(angle)
                        }
                    }.all { it }
            }
            // where everything is
            state = ArmState(
                ArmPosition(
                    JointPosition(waist.current()),
                    JointPosition(extender.current()),
                    JointPosition(gripper.current()),
                    JointPosition(elbow.current())
                ),
                true
            )
        }
    }
}
