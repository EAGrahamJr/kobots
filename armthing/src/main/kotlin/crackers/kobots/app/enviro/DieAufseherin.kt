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
import crackers.kobots.app.execution.goToSleep
import crackers.kobots.execution.*
import crackers.kobots.parts.ActionSequence
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.time.LocalTime

/*
 * Message central.
 */
object DieAufseherin {
    internal const val DA_TOPIC = "DieAufseherin"

    data class DasOverseerEvent(val dropOff: Boolean, val rtos: Boolean) : KobotsEvent

    // singletons events
    internal val dropOffComplete = DasOverseerEvent(true, false)
    internal val returnRequested = DasOverseerEvent(false, true)

    private val logger = LoggerFactory.getLogger("DieAufseherin")

    private var returnRequest: ActionSequence? = null

    fun setUpListeners() {
        localStuff()
        homeAssistantStuff()
    }

    private fun localStuff() {
        joinTopic(
            DA_TOPIC,
            KobotsSubscriber<DasOverseerEvent> {
                logger.info("Got a DA event: $it")
                if (it.rtos && returnRequest != null) {
                    TheArm.request(returnRequest!!)
                    returnRequest = null
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
                val sleepy = !payload.onOff("lamp")
                publishToTopic(SLEEP_TOPIC, SleepEvent(sleepy))
                if (sleepy) TheArm.request(goToSleep) else if (LocalTime.now().hour < 16) rotoSelect(0)
            }
        }
        haSub("zwave/Office/TriBaby/49/0/Air_temperature") { payload ->
            if (payload.has("value")) {
                val temp = payload.getDouble("value")
                VeryDumbThermometer.setTemperature(temp.toFloat())
            }
        }
        haSub("kobots/events") { payload ->
            if (payload.has("source")) {
                when (payload.getString("source")) {
                    "rotomatic" -> handleRotomatc(payload)
                    "proximity" -> if (runFlag.get()) {
                        publishToTopic(TheArm.REQUEST_TOPIC, allStop)
                        runFlag.set(false)
                    }
                }
            }
        }
        // zwave/nodeID_14/48/0/Motion
    }

    /**
     * What to do when the Rotomatic is selected.
     */
    private fun handleRotomatc(payload: JSONObject) {
        val rotoSelected = payload.getInt("selected")
        when {
            rotoSelected == 0 -> {
                returnRequest = PickWithRotomatic.standingPickupAndReturn
                PickWithRotomatic.moveStandingObjectToTarget
            }

            rotoSelected < 5 -> {
                returnRequest = PickWithRotomatic.thinItemReturn
                PickWithRotomatic.moveThinItemToTarget
            }

            else -> {
                logger.error("Unknown Rotomatic selection: $rotoSelected")
                null
            }
        }?.let { TheArm.request(it) }
    }

    private fun haSub(s: String, handler: (JSONObject) -> Unit) {
        mqtt.subscribe(s) { payload -> handler(JSONObject(payload)) }
    }
}
