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

import crackers.kobots.mqtt.KobotsMQTT


/**
 * Wrapper for the MQTT client and start alive-check
 */
internal val mqtt = KobotsMQTT("brainz", "tcp://192.168.1.4:1883").apply {
    startAliveCheck()
}

/**
 * Send a servo command to the servoMatic topic
 */
internal fun ServoMaticCommand.send() {
    mqtt.publish(SERVO_TOPIC, name)
}
