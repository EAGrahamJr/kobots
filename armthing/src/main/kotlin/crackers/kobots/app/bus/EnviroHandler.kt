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

package crackers.kobots.app.bus

import crackers.kobots.app.SLEEP_TOPIC
import crackers.kobots.app.SleepEvent
import crackers.kobots.app.VeryDumbThermometer
import crackers.kobots.app.arm.TheArm
import crackers.kobots.app.execution.EyeDropDemo
import crackers.kobots.app.onOff
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import org.tinylog.Logger
import java.time.LocalTime

/**
 * Starting integration with the home automation systems.
 */
object EnviroHandler {
    private val mqttClient = MqttClient("tcp://192.168.1.4:1883", "kobots", MemoryPersistence())

    fun startHandler() {
        val options = MqttConnectOptions().apply {
            connectionTimeout = 10
            isCleanSession = false
            isAutomaticReconnect = true
        }
        with(mqttClient) {
            connect(options)
            setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Logger.error(cause, "Connection lost")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    // ignore
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    // ignore
                }
            })
            handle("homebody/bedroom/casey") { payload ->
                if (payload.has("lamp")) {
                    if (LocalTime.now().hour == 22) TheArm.request(EyeDropDemo.pickupItem())
                }
            }
            handle("homebody/office/paper") { payload ->
                if (payload.has("lamp")) {
                    if (payload.onOff("lamp")) publishToTopic(SLEEP_TOPIC, SleepEvent(false))
                    else publishToTopic(SLEEP_TOPIC, SleepEvent(true))
                }
            }
            handle("zwave/nodeID_14/49/0/Air_temperature") { payload ->
                if (payload.has("value")) {
                    val temp = payload.getDouble("value")
                    VeryDumbThermometer.setTemperature(temp.toFloat())
                }
            }
        }

        // zwave/nodeID_14/48/0/Motion
    }

    private fun MqttClient.handle(topic: String, handler: (JSONObject) -> Unit) =
        subscribe(topic) { _, msg ->
            try {
                val payload = JSONObject(String(msg.payload))
                handler(payload)
            } catch (t: Throwable) {
                Logger.error(t, "That didn't work")
            }
        }
}
