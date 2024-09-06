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

import crackers.kobots.app.dostuff.Jeep
import crackers.kobots.app.dostuff.SuzerainOfServos.ELBOW_HOME
import crackers.kobots.app.dostuff.SuzerainOfServos.ELBOW_MAX
import crackers.kobots.app.dostuff.SuzerainOfServos.FINGERS_CLOSED
import crackers.kobots.app.dostuff.SuzerainOfServos.FINGERS_OPEN
import crackers.kobots.app.dostuff.SuzerainOfServos.PALM_DOWN
import crackers.kobots.app.dostuff.SuzerainOfServos.PALM_UP
import crackers.kobots.app.dostuff.SuzerainOfServos.SHOULDER_HOME
import crackers.kobots.app.dostuff.SuzerainOfServos.SHOULDER_MAX
import crackers.kobots.app.dostuff.SuzerainOfServos.WAIST_HOME
import crackers.kobots.app.dostuff.SuzerainOfServos.WAIST_MAX
import crackers.kobots.app.dostuff.SuzerainOfServos.elbow
import crackers.kobots.app.dostuff.SuzerainOfServos.fingers
import crackers.kobots.app.dostuff.SuzerainOfServos.shoulder
import crackers.kobots.app.dostuff.SuzerainOfServos.waist
import crackers.kobots.app.dostuff.SuzerainOfServos.wrist
import crackers.kobots.app.newarm.Predestination
import crackers.kobots.mqtt.homeassistant.*
import crackers.kobots.mqtt.homeassistant.KobotNumberEntity.Companion.NumberHandler
import crackers.kobots.parts.movement.DefaultActionSpeed
import crackers.kobots.parts.movement.LinearActuator
import crackers.kobots.parts.movement.Rotator
import crackers.kobots.parts.movement.sequence
import kotlin.math.roundToInt
import crackers.kobots.app.dostuff.SuzerainOfServos as Suzi

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

    private class ArmRotateHandler(val rotator: Rotator, val thing: String) : NumberHandler {
        override fun currentState() = rotator.current().toFloat()

        override fun set(target: Float) {
            Suzi does sequence {
                name = "HA Move $thing"
                action {
                    requestedSpeed = DefaultActionSpeed.SLOW
                    rotator rotate target.roundToInt()
                }
            }
        }
    }

    private class PctHandler(val linear: LinearActuator, val thing: String) : NumberHandler {
        override fun currentState() = linear.current().toFloat()

        override fun set(target: Float) {
            Suzi does sequence {
                name = "HA Move $thing"
                action {
                    requestedSpeed = DefaultActionSpeed.SLOW
                    linear goTo target.roundToInt()
                }
            }
        }
    }

    /**
     * Turn it sideways
     */
    private val waistEntity = object : KobotNumberEntity(
        object : NumberHandler {
            override fun currentState() = waist.current().toFloat()

            override fun set(target: Float) {
                Suzi does Predestination.whipIt(target.roundToInt(), waist, DefaultActionSpeed.VERY_FAST)
            }
        },
        "arm_waist",
        "Arm: Waist",
        haIdentifier,
        min = WAIST_HOME,
        max = WAIST_MAX,
        unitOfMeasurement = "degrees"
    ) {
        override val icon = "mdi:rotate-360"
    }

    /**
     * In and oot
     */
    private val shoulderEntity = object : KobotNumberEntity(
        ArmRotateHandler(shoulder, "Shoulder"),
        "arm_shoulder",
        "Arm: Shoulder",
        haIdentifier,
        min = SHOULDER_HOME,
        max = SHOULDER_MAX,
        unitOfMeasurement = "degrees",
        mode = Companion.DisplayMode.SLIDER
    ) {
        override val icon = "mdi:horizontal-rotate-clockwise"
    }

    /**
     * Hup down
     */
    private val elbowEntity = object : KobotNumberEntity(
        ArmRotateHandler(elbow, "Elbow"),
        "arm_elbow",
        "Arm: Elbow",
        haIdentifier,
        min = ELBOW_HOME,
        max = ELBOW_MAX,
        unitOfMeasurement = "degrees",
        mode = Companion.DisplayMode.SLIDER
    ) {
        override val icon = "mdi:horizontal-rotate-clockwise"
    }

    private val wristEntity = object : KobotNumberEntity(
        ArmRotateHandler(wrist, "Wrist"),
        "arm_wrist",
        "Arm: Wrist",
        haIdentifier,
        min = PALM_DOWN,
        max = PALM_UP,
        unitOfMeasurement = "degrees",
        mode = Companion.DisplayMode.SLIDER
    ) {
        override val icon = "mdi:hand-extended"
    }

    /**
     * Grabby thing
     */
    private val fingersEntity = object : KobotNumberEntity(
        PctHandler(fingers, "Fingers"),
        "arm_fingers",
        "Arm: Fingers",
        haIdentifier,
        min = FINGERS_OPEN,
        max = FINGERS_CLOSED,
        mode = Companion.DisplayMode.SLIDER
    ) {
        override val icon = "mdi:hand-coin"
    }


    val noodleEntity = KobotLight("small_nood", BasicLightController(Jeep.noodleLamp), "Da Nood", haIdentifier)

    private val armEntities = listOf(waistEntity, shoulderEntity, elbowEntity, wristEntity, fingersEntity)

    /**
     * LET'S LIGHT THIS THING UP!!!
     */
    override fun start() {
        noodleEntity.start()
        waistEntity.start()
        shoulderEntity.start()
        elbowEntity.start()
        wristEntity.start()
        fingersEntity.start()
        commandSelectEntity.start()
        tofSensor.start()
    }

    fun sendUpdatedStates() {
        armEntities.forEach { it.sendCurrentState() }
    }

    override fun stop() {
    }
}
