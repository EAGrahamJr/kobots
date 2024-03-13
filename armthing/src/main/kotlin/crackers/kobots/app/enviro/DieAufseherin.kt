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
import crackers.kobots.app.Startable
import crackers.kobots.app.enviro.HAStuff.crickitNeoPixel
import crackers.kobots.app.enviro.HAStuff.noodSwitch
import crackers.kobots.app.enviro.HAStuff.numberWaistEntity
import crackers.kobots.app.enviro.HAStuff.rosetteStrand
import crackers.kobots.app.enviro.HAStuff.selector
import crackers.kobots.app.enviro.HAStuff.textDosEntity
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

/*
 * Central control, of a sorts.
 */
object DieAufseherin : Startable {
    // system-wide "wat the hell is going on" stuff
    enum class SystemMode {
        IDLE,
        IN_MOTION,
        MANUAL,
        SHUTDOWN
    }

    private val theMode = AtomicReference(SystemMode.IDLE)

    // TODO this needs a semaphore so that only one thing can manipulate it at a time
    var currentMode: SystemMode
        get() = theMode.get()
        set(v) {
            theMode.set(v)
        }


    enum class GripperActions {
        HOME, SAY_HI, STOP, CLUCK, MANUAL
    }

    private val logger = LoggerFactory.getLogger("DieAufseherin")

    // rock 'n' roll ================================================================================================

    override fun start() {
        localStuff()
        mqttStuff()
        startDevices()
    }

    override fun stop() {
        VeryDumbThermometer.reset()
        noodSwitch.handleCommand("OFF")
//        rosetteStrand.handleCommand("OFF")
        crickitNeoPixel.off()
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
//            startAliveCheck()
            allowEmergencyStop()

            subscribeJSON("kobots_auto/office_enviro_temperature/state") { payload ->
                if (payload.has("state")) {
                    val temp = payload.optString("state", "75").toFloat()
                    VeryDumbThermometer.setTemperature(temp)
                }
            }
        }
    }

    internal fun actionTime(payload: GripperActions?) {
        when (payload) {
            GripperActions.STOP -> AppCommon.applicationRunning = false
            GripperActions.HOME -> currentMode = SystemMode.IDLE
            GripperActions.MANUAL -> currentMode = SystemMode.MANUAL
            GripperActions.CLUCK -> DisplayDos.cluck()
            else -> logger.warn("Unknown command: $payload")
        }
    }


    private fun startDevices() {
        // HA stuff
        noodSwitch.start()
        selector.start()
        rosetteStrand.start()
        textDosEntity.start()
        numberWaistEntity.start()
    }
}
