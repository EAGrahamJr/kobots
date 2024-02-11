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

import crackers.kobots.app.AppCommon.mqttClient
import crackers.kobots.app.SuzerainOfServos.SENSAI_MIN
import crackers.kobots.app.SuzerainOfServos.senseiRotor
import crackers.kobots.app.SuzerainOfServos.senseiServo
import crackers.kobots.devices.sensors.VL6180X
import crackers.kobots.mqtt.KobotsMQTT.Companion.KOBOTS_EVENTS
import crackers.kobots.parts.app.KobotSleep
import crackers.kobots.parts.movement.sequence
import org.json.JSONObject
import org.slf4j.LoggerFactory

object Sensei {
    private val logger = LoggerFactory.getLogger("Sensei")
    internal val toffle by lazy { VL6180X() }

    private fun VL6180X.distance(): Float = try {
        distanceCm
    } catch (e: Exception) {
        0f
    }

    data class SensaiEvent(val angle: Int, val distance: Float)

    // sweep to 180 and then back
    val simpleScan by lazy {
        val SCAN_MAX = 180

        sequence {
            name = "Simple Scan"
            action {
                senseiRotor rotate SENSAI_MIN
            }
            action {
                senseiRotor forwardUntil {
                    publishEvent()
                    senseiRotor.current() == SCAN_MAX
                }
            }
            action {
                senseiRotor backwardUntil {
                    publishEvent()
                    senseiRotor.current() == SENSAI_MIN
                }
            }
            action {
                senseiRotor rotate SENSAI_MIN
            }
        }
    }

    private fun publishEvent() {
        KobotSleep.millis(5)
        val event = SensaiEvent(senseiRotor.current(), toffle.distance())
        logger.debug("Servo = ${senseiServo.angle} $event")
        mqttClient.publish(KOBOTS_EVENTS, JSONObject(event))
        KobotSleep.millis(15)
    }
}
