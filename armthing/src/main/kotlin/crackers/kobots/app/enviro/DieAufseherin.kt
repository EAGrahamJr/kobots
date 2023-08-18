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

import crackers.kobots.app.*
import crackers.kobots.app.arm.TheArm
import crackers.kobots.app.execution.PickWithRotomatic
import crackers.kobots.execution.*
import crackers.kobots.parts.ActionSequence
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicBoolean

/*
 * Message central.
 */
object DieAufseherin {
    internal const val DA_TOPIC = "DieAufseherin"

    data class DasOverseerEvent(val dropOff: Boolean, val rtos: Boolean) : KobotsEvent

    // singletons events
    internal val dropOffComplete = DasOverseerEvent(true, false)
    internal val returnComplete = DasOverseerEvent(false, true)

    /**
     * Something is in the target area
     */
    private val _inTarget = AtomicBoolean(false)
    var inTargetArea: Boolean
        get() = _inTarget.get()
        private set(b) = _inTarget.set(b)
    private val returnInProgress = AtomicBoolean(false)
    private val logger = LoggerFactory.getLogger("DieAufseherin")

    private lateinit var returnRequest: ActionSequence

    fun setUpListeners() {
        localStuff()
        homeAssistantStuff()
    }

    private fun localStuff() {
        joinTopic(
            DA_TOPIC,
            KobotsSubscriber<DasOverseerEvent> {
                logger.info("Got a DA event: $it")
                inTargetArea = it.dropOff
                if (returnInProgress.get() && it.rtos) {
                    returnInProgress.set(false)
                }
                logger.info("inTargetArea: $inTargetArea, returnInProgress: ${returnInProgress.get()}")
            }
        )

        joinTopic(
            SensorSuite.PROXIMITY_TOPIC,
            KobotsSubscriber<SensorSuite.ProximityTrigger> {
                if (inTargetArea) {
                    if (returnInProgress.compareAndSet(false, true)) {
                        TheArm.request(returnRequest)
                    }
                } else {
                    // assume this is an emergency stop
                    publishToTopic(TheArm.REQUEST_TOPIC, allStop)
                    mqtt.publish("kobots/rotoMatic", "stop")
                    runFlag.set(false)
                }
            }
        )
    }

    private fun homeAssistantStuff() {
        haSub("homebody/bedroom/casey") { payload ->
            if (payload.has("lamp")) {
                if (LocalTime.now().hour == 22) {
                    rotoSelect(0)
                }
            }
        }
        haSub("homebody/office/paper") { payload ->
            if (payload.has("lamp")) {
                publishToTopic(SLEEP_TOPIC, SleepEvent(!payload.onOff("lamp")))
            }
        }
        haSub("zwave/Office/TriBaby/49/0/Air_temperature") { payload ->
            if (payload.has("value")) {
                val temp = payload.getDouble("value")
                VeryDumbThermometer.setTemperature(temp.toFloat())
            }
        }
        haSub("kobots/events") { payload ->
            if (payload.has("source") && payload.getString("source") == "rotomatic") {
                val rotoSelected = payload.getInt("selected")
                val request = when {
                    rotoSelected == 0 -> {
                        returnRequest = PickWithRotomatic.standingPickupAndReturn
                        PickWithRotomatic.moveStandingObjectToTarget
                    }

                    rotoSelected < 5 -> {
                        returnRequest = PickWithRotomatic.thinItemReturn
                        PickWithRotomatic.moveThinItemToTarget
                    }

                    else -> {
                        logger.error("Unknown rotomatic selection: $rotoSelected")
                        null
                    }
                }
                if (request != null) {
                    TheArm.request(request)
                }
            }
        }
        // zwave/nodeID_14/48/0/Motion
    }

    private fun haSub(s: String, handler: (JSONObject) -> Unit) {
        mqtt.subscribe(s) { payload -> handler(JSONObject(payload)) }
    }
}
