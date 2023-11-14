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
import crackers.kobots.app.arm.TheArm
import crackers.kobots.app.enviro.VeryDumbThermometer.TEMP_OFFSET
import crackers.kobots.app.execution.*
import crackers.kobots.app.execution.PickUpAndMoveStuff.moveEyeDropsToDropZone
import crackers.kobots.mqtt.KobotsMQTT.Companion.KOBOTS_EVENTS
import crackers.kobots.parts.app.publishToTopic
import crackers.kobots.parts.enumValue
import crackers.kobots.parts.onOff
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicBoolean

/*
 * Message central.
 */
object DieAufseherin {
    enum class GripperActions {
        PICKUP, RETURN, HOME, SAY_HI, STOP, EXCUSE_ME, SLEEP, FLASHLIGHT, SIMPLESCAN
    }

    private val logger = LoggerFactory.getLogger("DieAufseherin")

    fun start() {
        localStuff()
        mqttStuff()
    }

    fun stop() {
        VeryDumbThermometer.setTemperature(TEMP_OFFSET)
    }

    private fun localStuff() {
//        AppCommon.executor.scheduleAtFixedRate(5.seconds, 100.milliseconds, ::youHaveOneJob)
    }

    private fun mqttStuff() = with(AppCommon.mqttClient) {
        startAliveCheck()
        allowEmergencyStop()

        subscribeJSON("homebody/bedroom/casey") { payload ->
            // bedtime: do the eye drops thing
            if (payload.has("lamp") && LocalTime.now().hour == 22) doTheEyeDropsThing()
        }
        subscribeJSON("homebody/office/paper") { payload ->
            val sleepy = !payload.onOff("lamp")
            RosetteStatus.goToSleep = sleepy
            publishToTopic(AppCommon.SLEEP_TOPIC, AppCommon.SleepEvent(sleepy))
            // on wakeup, get drops
            if (!sleepy && LocalTime.now().hour < 10) doTheEyeDropsThing()
        }
        subscribeJSON("zwave/Office/TriBaby/49/0/Air_temperature") { payload ->
            if (payload.has("value")) {
                val temp = payload.getDouble("value")
                VeryDumbThermometer.setTemperature(temp.toFloat())
            }
        }

        // subscribe to the command channel
        subscribe("kobots/gripOMatic") { payload ->
            logger.info("Got a gripOMatic command: $payload")
            when (enumValue<GripperActions>(payload.uppercase())) {
                GripperActions.PICKUP -> doTheEyeDropsThing()
                GripperActions.RETURN -> doTheEyeDropsReturnThing()
                GripperActions.HOME -> TheArm.request(homeSequence)
                GripperActions.SAY_HI -> TheArm.request(sayHi)
                GripperActions.STOP -> AppCommon.applicationRunning = false
                GripperActions.EXCUSE_ME -> TheArm.request(excuseMe)
                GripperActions.SLEEP -> {
                    RosetteStatus.goToSleep = true
                    publishToTopic(AppCommon.SLEEP_TOPIC, AppCommon.SleepEvent(true))
                }

                GripperActions.FLASHLIGHT -> TheArm.nood = !TheArm.nood
                GripperActions.SIMPLESCAN -> TheArm.request(simpleScan)
                else -> logger.warn("Unknown command: $payload")
            }
        }

        subscribeJSON(KOBOTS_EVENTS) { payload ->
            logger.info("Received $payload")
            if (payload.optString("source") == "Suzie") manageSwirly(payload)
        }

        // TODO something when the motion sensor in the office is triggered
    }


    private val doingDrops = AtomicBoolean(false)

    private fun manageSwirly(payload: JSONObject) {
        when (payload.optString("sequence")) {
            "Swirly Max" -> {
                if (doingDrops.get() && !payload.optBoolean("started")) TheArm.request(moveEyeDropsToDropZone)
                else logger.info("Ignoring Swirly Max")
            }

            "Swirly Home" -> if (doingDrops.get() && !payload.optBoolean("started")) {
                logger.info("Eye drops done")
                doingDrops.set(false)
            }

            else -> {}
        }
    }

    private val SERVO_TOPIC = "kobots/servoMatic"

    private fun doTheEyeDropsThing() {
        logger.info("Doing the eye drops thing")
        doingDrops.set(true)
        AppCommon.mqttClient.publish(SERVO_TOPIC, "left")
    }

    private fun doTheEyeDropsReturnThing() {
        TheArm.request(PickUpAndMoveStuff.returnDropsToStorage)
    }
}
