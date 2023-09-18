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
import crackers.kobots.app.AppCommon.SLEEP_TOPIC
import crackers.kobots.app.AppCommon.onOff
import crackers.kobots.app.AppCommon.runFlag
import crackers.kobots.app.arm.TheArm
import crackers.kobots.app.execution.PickUpAndMoveStuff
import crackers.kobots.app.execution.ROTO_RETURN
import crackers.kobots.app.execution.goToSleep
import crackers.kobots.app.execution.homeSequence
import crackers.kobots.execution.KobotsEvent
import crackers.kobots.execution.KobotsSubscriber
import crackers.kobots.execution.joinTopic
import crackers.kobots.execution.publishToTopic
import crackers.kobots.parts.ActionSequence
import crackers.kobots.parts.SequenceExecutor
import crackers.kobots.parts.SequenceExecutor.Companion.INTERNAL_TOPIC
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
    internal val dropOffRequested = DasOverseerEvent(dropOff = true, rtos = false)
    internal val returnRequested = DasOverseerEvent(dropOff = false, rtos = true)

    private val logger = LoggerFactory.getLogger("DieAufseherin")

    private var returnRequest: ActionSequence? = null

    fun start() {
        localStuff()
        homeAssistantStuff()
    }

    fun stop() {
        // hmmm
    }

    private fun localStuff() {
        joinTopic(
            DA_TOPIC,
            KobotsSubscriber<DasOverseerEvent> {
                logger.info("Got a DA event: $it")
                if (it.rtos && returnRequest != null) {
                    TheArm.request(returnRequest!!)
                    returnRequest = null
                } else if (it.dropOff) {
                    returnRequest = PickUpAndMoveStuff.returnDropsToStorage
                    TheArm.request(PickUpAndMoveStuff.moveEyeDropsToDropZone)
                }
            }
        )
        joinTopic(
            INTERNAL_TOPIC,
            KobotsSubscriber<SequenceExecutor.SequenceCompleted> { msg ->
                logger.info("Sequence completed: $msg")
                when (msg.sequence) {
//                    ROTO_PICKUP -> _menuIndex.set(Menu.RETURN_PICK.ordinal)
                    ROTO_RETURN -> ServoMaticCommand.DOWN.send()
                    else -> {
                        // do nothing
                    }
                }
            }
        )
    }

    private fun homeAssistantStuff() {
        haSub("homebody/bedroom/casey") { payload ->
            if (payload.has("lamp")) {
                if (LocalTime.now().hour == 22) {
                    ServoMaticCommand.UP.send()
                }
            }
        }
        haSub("homebody/office/paper") { payload ->
            if (payload.has("lamp")) {
                val sleepy = !payload.onOff("lamp")
                RosetteStatus.goToSleep = sleepy
                publishToTopic(SLEEP_TOPIC, AppCommon.SleepEvent(sleepy))
                if (sleepy) {
                    TheArm.request(goToSleep)
                    ServoMaticCommand.SLEEP.send()
                } else {
                    ServoMaticCommand.WAKEY.send()
                    if (LocalTime.now().hour < 10) ServoMaticCommand.UP.send()
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
            if (payload.has("source")) {
                when (payload.getString("source")) {
                    "servomatic" -> handleRotomatc(payload)
                    "proximity" -> if (runFlag.get()) {
                        logger.error("Proximity sensor triggered")
//                        publishToTopic(TheArm.REQUEST_TOPIC, allStop)
//                        runFlag.set(false)
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
        if (payload.optBoolean("liftIsUp", false)) {
            LocalTime.now().hour.also { h ->
                if (h < 8 || h == 22) publishToTopic(DA_TOPIC, dropOffRequested)
            }
        } else {
            TheArm.request(homeSequence)
        }
    }

    private fun haSub(s: String, handler: (JSONObject) -> Unit) {
        mqtt.subscribe(s) { payload -> handler(JSONObject(payload)) }
    }
}
