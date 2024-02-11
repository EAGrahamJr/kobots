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

package crackers.kobots.app.enviro

import crackers.kobots.app.AppCommon
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

/*
 * Central control, of a sorts.
 */
object DieAufseherin {
    // system-wide "wat the hell is going on" stuff
    enum class SystemMode {
        IDLE,
        IN_MOTION,
        MANUAL,
        SHUTDOWN
    }

    private val theMode = AtomicReference(SystemMode.IDLE)
    var currentMode: SystemMode
        get() = theMode.get()
        private set(v) {
            theMode.set(v)
        }


    enum class GripperActions {
        HOME, SAY_HI, STOP, SLEEP, MANUAL
    }

    private val logger = LoggerFactory.getLogger("DieAufseherin")

    fun start() {
        localStuff()
        mqttStuff()
        // HA stuff
        noodSwitch.start()
        selector.start()
    }

    fun stop() {
        VeryDumbThermometer.reset()
        noodSwitch.handleCommand("OFF")
    }

    private fun localStuff() {
        with(AppCommon.hasskClient) {
            sensor("office_enviro_temperature").state().let { fullState ->
                VeryDumbThermometer.setTemperature(fullState.state.toFloat())
            }
        }
    }

    private fun mqttStuff() {
        with(AppCommon.mqttClient) {
            startAliveCheck()
            allowEmergencyStop()

            subscribeJSON("kobots_auto/office_enviro_temperature/state") { payload ->
                if (payload.has("state")) {
                    val temp = payload.optString("state", "75").toFloat()
                    VeryDumbThermometer.setTemperature(temp)
                }
            }
        }
    }


    fun actionTime(payload: GripperActions?) {
        when (payload) {
            GripperActions.STOP -> AppCommon.applicationRunning = false
            GripperActions.HOME -> currentMode = SystemMode.IDLE
            GripperActions.MANUAL -> currentMode = SystemMode.MANUAL
            GripperActions.SLEEP -> currentMode = SystemMode.IDLE
            else -> logger.warn("Unknown command: $payload")
        }
    }
}
