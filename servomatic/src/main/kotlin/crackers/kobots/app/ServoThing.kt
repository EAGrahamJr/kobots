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

package crackers.kobots.app

import com.diozero.devices.ServoController
import crackers.kobots.app.AppCommon.mqttClient
import crackers.kobots.parts.app.KobotSleep
import crackers.kobots.parts.app.publishToTopic
import crackers.kobots.parts.movement.SequenceExecutor.Companion.MQTT_TOPIC
import crackers.kobots.parts.movement.SequenceRequest
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

/**
 * Handles a bunch of different servos for various things. Everything should have an MQTT interface.
 *
 */
val hat by lazy { ServoController() }

val logger = LoggerFactory.getLogger("Servomatic")

/**
 * Whatever
 */
const val REMOTE_PI = "diozero.remote.hostname"
const val SERVO_TOPIC = "kobots/servoMatic"

enum class ServoMaticCommand {
    STOP, UP, DOWN, LEFT, RIGHT, CENTER, SLEEP, WAKEY;
}

fun main(args: Array<String>?) {
    // pass any arg and we'll use the remote pi
    // NOTE: this reqquires a diozero daemon running on the remote pi and the diozero remote jar in the classpath
    if (args?.isNotEmpty() == true) System.setProperty(REMOTE_PI, args[0])

    mqttClient.apply {
        subscribe(SERVO_TOPIC) {
            val payload = ServoMaticCommand.valueOf(it.uppercase())
            when (payload) {
                ServoMaticCommand.STOP -> {
                    logger.error("Stopping via MQTT")
                    AppCommon.applicationRunning = false
                }

                else -> {}
            }
        }

        subscribeJSON(MQTT_TOPIC) { payload ->
            logger.info("Received $payload")
            when (payload.getString("sequence")) {
                "LocationPickup" -> if (payload.getBoolean("started")) publishToTopic(
                    SuzerainOfServos.SERVO_TOPIC,
                    SequenceRequest(steveTurns)
                )

                "ReturnPickup" -> if (!payload.getBoolean("started")) publishToTopic(
                    SuzerainOfServos.SERVO_TOPIC,
                    SequenceRequest(steveGoesHome)
                )

                else -> {}
            }
        }

        startAliveCheck()
    }

    PadPrincipal.start()
    hat.use { hat ->
//        logger.info("luminosity at start ${proxy.luminosity}")
        AppCommon.awaitTermination()

        KobotSleep.seconds(1)
    }
    PadPrincipal.stop()
    logger.warn("Servomatic exit")
    exitProcess(0)
}
