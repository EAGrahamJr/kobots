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
import crackers.kobots.app.otherstuff.Jeep
import crackers.kobots.mqtt.homeassistant.*
import crackers.kobots.parts.movement.*
import kotlin.math.roundToInt
import crackers.kobots.app.SuzerainOfServos as Suzi

/**
 * HA entities, etc.
 */
object HAJunk : Startable {

    private val haIdentifier = DeviceIdentifier("Kobots", "Servomatic")


    private val commandSelectEntity = KobotSelectEntity(Commando, "servo_selector", "Servo Selector", haIdentifier)

    val tofSensor = KobotAnalogSensor(
        "servomatic_tof",
        "Bobbi Detector",
        haIdentifier,
        KobotAnalogSensor.Companion.AnalogDevice.DISTANCE,
        unitOfMeasurement = "mm"
    )
    val ambientSensor = object : KobotAnalogSensor(
        "ambient_light",
        "Luminosity",
        haIdentifier,
        deviceClass = KobotAnalogSensor.Companion.AnalogDevice.ILLUMINANCE,
        unitOfMeasurement = "lumens"
    ) {
        override val icon = "mdi:lightbulb-alert"
    }

    private class ArmRotateHandler(val rotator: Rotator, val thing: String) :
        KobotNumberEntity.Companion.NumberHandler {
        override fun currentState() = rotator.current().toFloat()

        override fun set(target: Float) {
            val requested = sequence {
                name = "HA Move $thing"
                action {
                    requestedSpeed = DefaultActionSpeed.SLOW
                    rotator rotate target.roundToInt()
                }
            }
            Suzi.handleRequest(SequenceRequest(requested))
        }
    }

    private class PctHandler(val linear: LinearActuator, val thing: String) :
        KobotNumberEntity.Companion.NumberHandler {
        override fun currentState() = linear.current().toFloat()

        override fun set(target: Float) {
            val requested = sequence {
                name = "HA Move $thing"
                action {
                    requestedSpeed = DefaultActionSpeed.SLOW
                    linear goTo target.roundToInt()
                }
            }
            Suzi.handleRequest(SequenceRequest(requested))
        }
    }

    /**
     * Turn it sideways
     */
    private val swingEntity = object : KobotNumberEntity(
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
    private val boomEntity = object : KobotNumberEntity(
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
    private val armEntity = object : KobotNumberEntity(
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
    private val gripperEntity = object : KobotNumberEntity(
        PctHandler(gripper, "Gripper"),
        "arm_gripper",
        "Arm: Gripper",
        haIdentifier
    ) {}

    private val bucketEntity = object : KobotNumberEntity(
        ArmRotateHandler(bucketLink, "Bucket"),
        "arm_bucket",
        "Arm: Bucket",
        haIdentifier,
        min = BUCKET_HOME,
        max = BUCKET_MAX,
        unitOfMeasurement = "degrees"
    ) {}

    val noodleEntity = KobotLight("small_nood", BasicLightController(Jeep.noodleLamp), "Da Nood", haIdentifier)

    /**
     * LET'S LIGHT THIS THING UP!!!
     */
    override fun start() {
        noodleEntity.start()
        swingEntity.start()
        boomEntity.start()
        armEntity.start()
        gripperEntity.start()
        bucketEntity.start()
        commandSelectEntity.start()
        tofSensor.start()
        ambientSensor.start()
    }

    fun sendUpdatedStates() {
        swingEntity.sendCurrentState()
        boomEntity.sendCurrentState()
        armEntity.sendCurrentState()
        gripperEntity.sendCurrentState()
        bucketEntity.sendCurrentState()
    }

    override fun stop() {
    }
}
