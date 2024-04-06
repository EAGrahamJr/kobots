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

import com.diozero.devices.ServoController
import crackers.kobots.devices.MG90S_TRIM
import crackers.kobots.parts.movement.SequenceExecutor
import crackers.kobots.parts.movement.ServoLinearActuator
import crackers.kobots.parts.movement.ServoRotator
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * All the things
 */
object SuzerainOfServos : SequenceExecutor("Suzie", AppCommon.mqttClient), Startable {
    internal const val INTERNAL_TOPIC = "Servo.Suzie"
    private val hat by lazy {
        ServoController()
    }

    override fun start() {
//        TODO("Not yet implemented")
    }

    private lateinit var stopLatch: CountDownLatch
    override fun stop() {
        stopLatch = CountDownLatch(1)
        // TODO request all things "home"
        if (!stopLatch.await(30, TimeUnit.SECONDS)) {
            logger.error("Arm not homed in 30 seconds")
        }
        // forces everything to stop
        super.stop()
    }

    override fun canRun() = AppCommon.applicationRunning || ::stopLatch.isInitialized

    override fun preExecution() {
        systemState = SystemState.MOVING
    }

    override fun postExecution() {
        with(HAJunk) {
            swingEntity.sendCurrentState()
            boomEntity.sendCurrentState()
            armEntity.sendCurrentState()
            gripperEntity.sendCurrentState()
        }
        super.postExecution()
        systemState = SystemState.IDLE
        if (::stopLatch.isInitialized) stopLatch.countDown()
    }

    const val SWING_HOME = 0
    const val SWING_MAX = 140

    const val BOOM_UP = 0
    const val BOOM_DOWN = 90

    const val ARM_DOWN = -53
    const val ARM_UP = 65

    const val GRIPPER_OPEN = 0
    const val GRIPPER_CLOSED = 100


    // hardware! =====================================================================================================

    private val maxServoRange = IntRange(0, 180)

    val swing by lazy {
        val servo = hat.getServo(0, MG90S_TRIM, 0)
        val physicalRange = IntRange(SWING_HOME, SWING_MAX)
        ServoRotator(servo, physicalRange, maxServoRange)
    }

    val boomLink by lazy {
        val servo = hat.getServo(1, MG90S_TRIM, 0)
        val physicalRange = IntRange(BOOM_UP, BOOM_DOWN)
        ServoRotator(servo, physicalRange, maxServoRange)
    }

    val armLink by lazy {
        val servo = hat.getServo(2, MG90S_TRIM, 0)
        val physicalRange = IntRange(ARM_DOWN, ARM_UP)
        ServoRotator(servo, physicalRange, maxServoRange)
    }

    val gripper by lazy {
        val servo = hat.getServo(3, MG90S_TRIM, 0)
        ServoLinearActuator(servo, 0f, 80f)
    }
}
