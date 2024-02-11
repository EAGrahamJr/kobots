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
import crackers.kobots.app.AppCommon.REMOTE_PI
import crackers.kobots.app.AppCommon.mqttClient
import crackers.kobots.app.Sensei.simpleScan
import crackers.kobots.app.Sensei.toffle
import crackers.kobots.app.SuzerainOfServos.INTERNAL_TOPIC
import crackers.kobots.mqtt.KobotsMQTT.Companion.KOBOTS_EVENTS
import crackers.kobots.parts.app.KobotSleep
import crackers.kobots.parts.app.publishToTopic
import crackers.kobots.parts.enumValue
import crackers.kobots.parts.movement.ActionSequence
import crackers.kobots.parts.movement.SequenceRequest
import org.json.JSONObject
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

/**
 * Handles a bunch of different servos for various things. Everything should have an MQTT interface.
 */
val hat by lazy { ServoController() }

val logger = LoggerFactory.getLogger("Servomatic")

/**
 * Whatever
 */
const val SERVO_TOPIC = "kobots/servoMatic"

enum class ServoMaticCommand {
    STOP, HOME, DROPS, SCAN;
}

fun main(args: Array<String>?) {
    // pass any arg and we'll use the remote pi
    // NOTE: this reqquires a diozero daemon running on the remote pi and the diozero remote jar in the classpath
    if (args?.isNotEmpty() == true) System.setProperty(REMOTE_PI, args[0])

    // fire off the sensor just to make sure it's working
    toffle.distanceCm

    mqttClient.apply {
        subscribe(SERVO_TOPIC) {
            logger.info("Servo received command $it")
            val payload = enumValue<ServoMaticCommand>(it.uppercase().trim())
            when (payload) {
                ServoMaticCommand.STOP -> {
                    logger.error("Stopping via MQTT")
                    AppCommon.applicationRunning = false
                }

                ServoMaticCommand.DROPS -> servoRequest(swirlyMax)
                ServoMaticCommand.HOME -> servoRequest(swirlyHome)
                ServoMaticCommand.SCAN -> servoRequest(simpleScan)

                else -> logger.warn("No clue about $payload")
            }
        }

        subscribeJSON(KOBOTS_EVENTS) { payload: JSONObject ->
            when (payload.optString("source")) {
                "TheArm" -> doArmThing(payload)
                "Proximity" -> doAlertThing(payload)
                else -> logger.debug("Received $payload")
            }
        }

        startAliveCheck()
        allowEmergencyStop()
    }

//    OrreryThing.start()
    // TODO the Sparkfun I2c port on the servo hat is not working
    AppCommon.awaitTermination()
    KobotSleep.seconds(1)
    hat.close()
    logger.warn("Servomatic exit")
    exitProcess(0)
}

internal fun servoRequest(sequence: ActionSequence) = publishToTopic(INTERNAL_TOPIC, SequenceRequest(sequence))
