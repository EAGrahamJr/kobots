/*
 * Copyright 2022-2026 by E. A. Graham, Jr.
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

package crackers.kobots.app.mechanicals

import com.diozero.api.ServoTrim
import com.diozero.devices.PCA9685
import com.diozero.devices.ServoController
import com.diozero.devices.sandpit.motor.BasicStepperController.StepStyle
import crackers.kobots.app.AppCommon
import crackers.kobots.app.HAJunk
import crackers.kobots.app.SystemState
import crackers.kobots.app.newarm.Predestination
import crackers.kobots.app.systemState
import crackers.kobots.devices.set
import crackers.kobots.parts.movement.BasicStepperRotator
import crackers.kobots.parts.movement.LimitedRotator.Companion.rotator
import crackers.kobots.parts.movement.SequenceExecutor
import crackers.kobots.parts.movement.SequenceRequest
import crackers.kobots.parts.movement.ServoLinearActuator
import crackers.kobots.parts.off
import crackers.kobots.parts.on
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * All the things
 */
object SuzerainOfServos : SequenceExecutor("Suzie", AppCommon.mqttClient), AppCommon.Startable {
    internal const val INTERNAL_TOPIC = "Servo.Suzie"
    private val hat by lazy {
        ServoController(PCA9685(I2CFactory.suziDevice))
    }

    override fun start() {
//        TODO("Not yet implemented")
    }

    private lateinit var stopLatch: CountDownLatch

    override fun stop() {
        // already did this
        SuzerainOfServos::stopLatch.isInitialized && return

        // forces everything to stop
        super.stop()

        stopLatch = CountDownLatch(1)
        handleRequest(SequenceRequest(Predestination.homeSequence))
        if (!stopLatch.await(30, TimeUnit.SECONDS)) {
            logger.error("Arm not homed in 30 seconds")
        }
    }

    override fun canRun() = AppCommon.applicationRunning && systemState != SystemState.MANUAL

    override fun preExecution() {
        systemState = SystemState.MOVING
        Jeep.noodleLamp set on
        logger.info("Executing ${currentSequence.get()}")
        super.preExecution()
    }

    override fun postExecution() {
        waist.release()
        Jeep.noodleLamp set off
        HAJunk.sendUpdatedStates()
        super.postExecution()
        systemState = SystemState.IDLE
        if (SuzerainOfServos::stopLatch.isInitialized) stopLatch.countDown()
    }

    const val WAIST_HOME = 0
    const val WAIST_MAX = 360

    const val SHOULDER_HOME = 0
    const val SHOULDER_MAX = 135

    const val ELBOW_HOME = 0
    const val ELBOW_MAX = 135

    const val WRIST_REST = 0
    const val WRIST_COCKED = 90

    const val FINGERS_OPEN = 0
    const val FINGERS_CLOSED = 100

    // hardware! =====================================================================================================

    private val maxServoRange = 0..ServoTrim.MG90S.maxAngle

    // TODO D-H frame coordinate system, these are almost all _reversed_ for right-hand coordinate system
    val shoulder by lazy {
        hat.getServo(0, ServoTrim.MG90S, 0).rotator(SHOULDER_HOME..SHOULDER_MAX)
    }

    val elbow by lazy {
        hat.getServo(1, ServoTrim.MG90S, 0).rotator(ELBOW_HOME..ELBOW_MAX)
    }

    val wrist by lazy {
        hat.getServo(2, ServoTrim.MG90S, 0).rotator(WRIST_REST..WRIST_COCKED)
    }

    val fingers: ServoLinearActuator by lazy {
        ServoLinearActuator(hat.getServo(3, ServoTrim.MG90S, 0), 0f, 150f)
    }

    // technically not a servo
    val waist by lazy {
        BasicStepperRotator(
            Jeep.stepper1,
            gearRatio = 2.25f, // the omg how frickin' big is this thing turntable 40-90
            stepStyle = StepStyle.INTERLEAVE,
            stepsPerRotation = 4096,
        )
    }
}
