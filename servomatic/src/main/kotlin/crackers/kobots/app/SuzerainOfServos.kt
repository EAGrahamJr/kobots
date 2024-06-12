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

package crackers.kobots.app

import com.diozero.api.ServoTrim
import com.diozero.devices.PCA9685
import com.diozero.devices.ServoController
import crackers.kobots.app.newarm.Predestination
import crackers.kobots.app.otherstuff.Jeep
import crackers.kobots.devices.set
import crackers.kobots.parts.movement.LimitedRotator.Companion.rotator
import crackers.kobots.parts.movement.SequenceExecutor
import crackers.kobots.parts.movement.SequenceRequest
import crackers.kobots.parts.movement.ServoLinearActuator
import crackers.kobots.parts.movement.ServoRotator
import crackers.kobots.parts.off
import crackers.kobots.parts.on
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * All the things
 */
object SuzerainOfServos : SequenceExecutor("Suzie", AppCommon.mqttClient), Startable {
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
        ::stopLatch.isInitialized && return

        // forces everything to stop
        super.stop()

        logger.info("Setting latch")
        stopLatch = CountDownLatch(1)
        val fullStopSequence = Predestination.gripperOpen + Predestination.outOfTheWay + Predestination.homeSequence
        handleRequest(SequenceRequest(fullStopSequence))
        if (!stopLatch.await(30, TimeUnit.SECONDS)) {
            logger.error("Arm not homed in 30 seconds")
        }
    }

    override fun canRun() = AppCommon.applicationRunning || ::stopLatch.isInitialized

    override fun preExecution() {
        systemState = SystemState.MOVING
        Jeep.noodleLamp set on
        logger.info("Executing ${currentSequence.get()}")
        super.preExecution()
    }

    override fun postExecution() {
        Jeep.noodleLamp set off
        HAJunk.sendUpdatedStates()
        super.postExecution()
        systemState = SystemState.IDLE
        if (::stopLatch.isInitialized) stopLatch.countDown()
    }

    const val SWING_HOME = 0
    const val SWING_MAX = 133

    const val BOOM_UP = 0
    const val BOOM_HOME = 0
    const val BOOM_DOWN = 70

    const val ARM_DOWN = 0
    const val ARM_HOME = 0
    const val ARM_UP = 90

    const val GRIPPER_OPEN = 0
    const val GRIPPER_CLOSED = 100

    const val BUCKET_HOME = 0
    const val BUCKET_MAX = 180

    // hardware! =====================================================================================================

    private val maxServoRange = 0..ServoTrim.MG90S.maxAngle

    // TODO D-H frame coordinate system, these are almost all _reversed_ for right-hand coordinate system
    val swing by lazy {
        hat.getServo(0, ServoTrim.MG90S, 0).rotator(SWING_HOME..SWING_MAX, maxServoRange)
    }

    val boomLink by lazy {
        hat.getServo(1, ServoTrim.MG90S, 0).rotator(BOOM_UP..BOOM_DOWN, 0..140)
    }

    val armLink by lazy {
        hat.getServo(2, ServoTrim.MG90S, 0).rotator(ARM_DOWN..ARM_UP, maxServoRange)
    }

    val gripper by lazy {
        hat.getServo(3, ServoTrim.MG90S, 0).let { servo -> ServoLinearActuator(servo, 0f, 80f) }
    }

    val bucketLink by lazy {
        val servo = hat.getServo(4, ServoTrim.MG90S, 0)
        val range = 0..BUCKET_MAX
        object : ServoRotator(servo, range) {
            override fun rotateTo(angle: Int) = (armLink.current() <= 5) || super.rotateTo(angle)
        }
    }
}
