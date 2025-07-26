/*
 * Copyright 2022-2025 by E. A. Graham, Jr.
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

import crackers.kobots.app.mechanicals.Jeep
import crackers.kobots.app.mechanicals.SuzerainOfServos.ELBOW_HOME
import crackers.kobots.app.mechanicals.SuzerainOfServos.ELBOW_MAX
import crackers.kobots.app.mechanicals.SuzerainOfServos.FINGERS_CLOSED
import crackers.kobots.app.mechanicals.SuzerainOfServos.FINGERS_OPEN
import crackers.kobots.app.mechanicals.SuzerainOfServos.SHOULDER_HOME
import crackers.kobots.app.mechanicals.SuzerainOfServos.SHOULDER_MAX
import crackers.kobots.app.mechanicals.SuzerainOfServos.WAIST_HOME
import crackers.kobots.app.mechanicals.SuzerainOfServos.WAIST_MAX
import crackers.kobots.app.mechanicals.SuzerainOfServos.WRIST_COCKED
import crackers.kobots.app.mechanicals.SuzerainOfServos.WRIST_REST
import crackers.kobots.app.mechanicals.SuzerainOfServos.elbow
import crackers.kobots.app.mechanicals.SuzerainOfServos.fingers
import crackers.kobots.app.mechanicals.SuzerainOfServos.shoulder
import crackers.kobots.app.mechanicals.SuzerainOfServos.waist
import crackers.kobots.app.mechanicals.SuzerainOfServos.wrist
import crackers.kobots.mqtt.homeassistant.BasicLightController
import crackers.kobots.mqtt.homeassistant.DeviceIdentifier
import crackers.kobots.mqtt.homeassistant.KobotLight
import crackers.kobots.mqtt.homeassistant.KobotSelectEntity
import crackers.kobots.parts.movement.toSpeed
import crackers.kobots.robot.remote.HAThingFactory
import crackers.kobots.app.mechanicals.SuzerainOfServos as Suzi

/**
 * HA entities, etc.
 */
object HAJunk : AppCommon.Startable {
    private val haIdentifier = DeviceIdentifier("Kobots", "Servomatic")
    private val maker = HAThingFactory("Arm", haIdentifier, Suzi)

    private val commandSelectEntity = KobotSelectEntity(Commando, "servo_selector", "Servo Selector", haIdentifier)

    /**
     * Turn it sideways
     */
    private val waistEntity =
        maker.rotateThing("Waist", waist, WAIST_HOME, WAIST_MAX, "mdi:rotate-360", 1L.toSpeed())

    /**
     * In and oot
     * Hup down
     */
    private val shoulderEntity =
        maker.rotateThing("Shoulder", shoulder, SHOULDER_HOME, SHOULDER_MAX, "mdi:horizontal-rotate-clockwise")
    private val elbowEntity =
        maker.rotateThing("Elbow", elbow, ELBOW_HOME, ELBOW_MAX, "mdi:horizontal-rotate-clockwise")

    /**
     * End of things
     */
    private val wristEntity = maker.rotateThing("Wrist", wrist, WRIST_REST, WRIST_COCKED, "mdi:hand-extended")

    /**
     * Grabby thing
     */
    private val fingersEntity = maker.linearThing("Fingers", fingers, FINGERS_OPEN, FINGERS_CLOSED, "mdi:hand-coin")

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
    }

    fun sendUpdatedStates() {
        commandSelectEntity.sendCurrentState("None")
        armEntities.forEach { it.sendCurrentState() }
    }

    override fun stop() {
    }
}
