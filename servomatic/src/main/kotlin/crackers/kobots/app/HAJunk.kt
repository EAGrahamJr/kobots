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

import crackers.kobots.app.SuzerainOfServos.ARM_DOWN
import crackers.kobots.app.SuzerainOfServos.ARM_UP
import crackers.kobots.app.SuzerainOfServos.BOOM_DOWN
import crackers.kobots.app.SuzerainOfServos.BOOM_UP
import crackers.kobots.app.SuzerainOfServos.BUCKET_HOME
import crackers.kobots.app.SuzerainOfServos.BUCKET_MAX
import crackers.kobots.app.SuzerainOfServos.SWING_HOME
import crackers.kobots.app.SuzerainOfServos.SWING_MAX
import crackers.kobots.app.SuzerainOfServos.armLink
import crackers.kobots.app.SuzerainOfServos.boomLink
import crackers.kobots.app.SuzerainOfServos.bucketLink
import crackers.kobots.app.SuzerainOfServos.gripper
import crackers.kobots.app.SuzerainOfServos.swing
import crackers.kobots.mqtt.homeassistant.DeviceIdentifier
import crackers.kobots.mqtt.homeassistant.KobotAnalogSensor
import crackers.kobots.mqtt.homeassistant.KobotNumberEntity
import crackers.kobots.mqtt.homeassistant.KobotSelectEntity
import crackers.kobots.parts.enumValue
import crackers.kobots.parts.movement.*
import kotlin.math.roundToInt

/**
 * HA entities, etc.
 */
object HAJunk : AutoCloseable {
    private val haIdentifier = DeviceIdentifier("Kobots", "Servomatic")

    private val selectHandler = object : KobotSelectEntity.Companion.SelectHandler {
        override val options = Mode.entries.map { it.name }
        override fun executeOption(select: String) {
            when (enumValue<Mode>(select)) {
                Mode.IDLE -> {
                }

                Mode.STOP -> AppCommon.applicationRunning = false
                Mode.CLUCK -> {
                }

                Mode.HOME -> SuzerainOfServos.handleRequest(SequenceRequest(Predestination.homeSequence))
                Mode.SAY_HI -> SuzerainOfServos.handleRequest(SequenceRequest(Predestination.sayHi))

                else -> logger.warn("No clue what to do with $select")
            }
        }
    }

    val commandSelectEntity = KobotSelectEntity(selectHandler, "servo_selector", "Servo Selector", haIdentifier)

    val tofSensor = KobotAnalogSensor(
        "servomatic_tof", "Bobbi Detector", haIdentifier,
        KobotAnalogSensor.Companion.AnalogDevice.DISTANCE, unitOfMeasurement = "mm"
    )

    private class ArmRotateHandler(val rotator: Rotator, val thing: String) :
        KobotNumberEntity.Companion.NumberHandler {
        override fun currentState() = rotator.current().toFloat()

        override fun move(target: Float) {
            crackers.kobots.parts.movement.sequence {
                name = "HA Move $thing"
                action {
                    requestedSpeed = DefaultActionSpeed.SLOW
                    rotator rotate target.roundToInt()
                }
            }.run {
                SuzerainOfServos.handleRequest(SequenceRequest(this))
            }
        }
    }

    private class PctHandler(val linear: LinearActuator, val thing: String) :
        KobotNumberEntity.Companion.NumberHandler {
        override fun currentState() = linear.current().toFloat()

        override fun move(target: Float) {
            sequence {
                name = "HA Move $thing"
                action {
                    requestedSpeed = DefaultActionSpeed.SLOW
                    linear goTo target.roundToInt()
                }
            }.run {
                SuzerainOfServos.handleRequest(SequenceRequest(this))
            }
        }
    }

    /**
     * Turn it sideways
     */
    val swingEntity = object : KobotNumberEntity(
        ArmRotateHandler(swing, "Swing"),
        "arm_swing",
        "Arm: Swing",
        haIdentifier,
        min = SWING_HOME,
        max = SWING_MAX,
        unitOfMeasurement = "degrees"
    ) {
        override val icon = "mdi:rotate-360"
    }

    /**
     * In and oot
     */
    val boomEntity = object : KobotNumberEntity(
        ArmRotateHandler(boomLink, "Boom"),
        "arm_boom",
        "Arm: Boom",
        haIdentifier,
        min = BOOM_UP,
        max = BOOM_DOWN,
        unitOfMeasurement = "degrees"
    ) {
        override val icon = "mdi:horizontal-rotate-clockwise"
    }

    /**
     * Hup down
     */
    val armEntity = object : KobotNumberEntity(
        ArmRotateHandler(armLink, "Arm"),
        "arm_arm",
        "Arm: Arm",
        haIdentifier,
        min = ARM_DOWN,
        max = ARM_UP,
        unitOfMeasurement = "degrees"
    ) {
        override val icon = "mdi:horizontal-rotate-clockwise"
    }

    /**
     * Grabby thing
     */
    val gripperEntity = object : KobotNumberEntity(
        PctHandler(gripper, "Gripper"),
        "arm_gripper",
        "Arm: Gripper",
        haIdentifier
    ) {}

    val bucketEntity = object : KobotNumberEntity(
        ArmRotateHandler(bucketLink, "Bucket"),
        "arm_bucket",
        "Arm: Bucket",
        haIdentifier,
        min = BUCKET_HOME,
        max = BUCKET_MAX,
        unitOfMeasurement = "degrees"
    ) {}

    /**
     * LET'S LIGHT THIS THING UP!!!
     */
    fun start() {
        swingEntity.start()
        boomEntity.start()
        armEntity.start()
        gripperEntity.start()
        bucketEntity.start()
        commandSelectEntity.start()
        tofSensor.start()
    }

    fun sendUpdatedStates() {
        swingEntity.sendCurrentState()
        boomEntity.sendCurrentState()
        armEntity.sendCurrentState()
        gripperEntity.sendCurrentState()
        bucketEntity.sendCurrentState()
    }

    override fun close() {
    }
}
