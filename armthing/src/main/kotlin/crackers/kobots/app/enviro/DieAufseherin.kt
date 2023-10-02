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

package crackers.kobots.app.enviro

import crackers.kobots.app.AppCommon
import crackers.kobots.app.AppCommon.SLEEP_TOPIC
import crackers.kobots.app.EVENT_TOPIC
import crackers.kobots.app.arm.TheArm
import crackers.kobots.app.execution.*
import crackers.kobots.app.mqtt
import crackers.kobots.devices.sensors.VCNL4040
import crackers.kobots.parts.app.KobotsSubscriber
import crackers.kobots.parts.app.joinTopic
import crackers.kobots.parts.app.publishToTopic
import crackers.kobots.parts.movement.SequenceExecutor
import crackers.kobots.parts.movement.SequenceExecutor.Companion.INTERNAL_TOPIC
import crackers.kobots.parts.onOff
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/*
 * Message central.
 */
object DieAufseherin {
    enum class GripperActions {
        PICKUP, RETURN, HOME, SAY_HI, STOP, EXCUSE_ME, SLEEP
    }

    private val logger = LoggerFactory.getLogger("DieAufseherin")

    fun start() {
        localStuff()
        mqttStuff()
    }

    fun stop() {
        // hmmm
    }

    private fun localStuff() {
        joinTopic(
            INTERNAL_TOPIC,
            KobotsSubscriber<SequenceExecutor.SequenceCompleted> { msg ->
                logger.info("Sequence completed: $msg")
            }
        )
        AppCommon.executor.scheduleAtFixedRate(::youHaveOneJob, 5000, 100, TimeUnit.MICROSECONDS)
    }

    const val CLOSE_ENOUGH = 15 // this is **approximately**  25mm
    const val PROXIMITY_TOPIC = "Prox.TooClose"

    private val sensor by lazy {
        VCNL4040().apply {
            proximityEnabled = true
            ambientLightEnabled = true
        }
    }
    private var wasTriggered = false
    private fun youHaveOneJob() {
        try {
            val prox = sensor.proximity
            wasTriggered = if (prox > CLOSE_ENOUGH) {
                if (!wasTriggered) mqtt.publish("kobots/buttonboard", "NEXT_FRONTBENCH")
                true
            } else false

        } catch (e: Exception) {
            logger.error("Proximity sensor error: ${e.message}")
        }
    }

    private fun mqttStuff() {
        haSub("homebody/bedroom/casey") { payload ->
            if (payload.has("lamp")) {
                if (LocalTime.now().hour == 22) {
                    TheArm.request(PickUpAndMoveStuff.moveEyeDropsToDropZone)
                }
            }
        }
        haSub("homebody/office/paper") { payload ->
            if (payload.has("lamp")) {
                val sleepy = !payload.onOff("lamp")
                RosetteStatus.goToSleep = sleepy
                publishToTopic(SLEEP_TOPIC, AppCommon.SleepEvent(sleepy))
                if (sleepy) {
                    TheArm.request(armSleep)
                } else {
                    if (LocalTime.now().hour < 10) TheArm.request(PickUpAndMoveStuff.moveEyeDropsToDropZone)
                }
            }
        }
        haSub("zwave/Office/TriBaby/49/0/Air_temperature") { payload ->
            if (payload.has("value")) {
                val temp = payload.getDouble("value")
                VeryDumbThermometer.setTemperature(temp.toFloat())
            }
        }
        haSub(EVENT_TOPIC) { payload ->
            logger.info("Got an event")
        }

        // subscribe to the command channel
        mqtt.subscribe("kobots/gripOMatic") { payload ->
            logger.info("Got a gripOMatic command: $payload")
            when (GripperActions.valueOf(payload.uppercase())) {
                GripperActions.PICKUP -> TheArm.request(PickUpAndMoveStuff.moveEyeDropsToDropZone)
                GripperActions.RETURN -> TheArm.request(PickUpAndMoveStuff.returnDropsToStorage)
                GripperActions.HOME -> TheArm.request(homeSequence)
                GripperActions.SAY_HI -> TheArm.request(sayHi)
                GripperActions.STOP -> AppCommon.applicationRunning = false
                GripperActions.EXCUSE_ME -> TheArm.request(excuseMe)
                GripperActions.SLEEP -> {
                    RosetteStatus.goToSleep = true
                    publishToTopic(SLEEP_TOPIC, AppCommon.SleepEvent(true))
                }
            }
        }

        // TODO something when the motion sensor in the office is triggered
    }

    private fun haSub(s: String, handler: (JSONObject) -> Unit) {
        mqtt.subscribe(s) { payload -> handler(JSONObject(payload)) }
    }
}
