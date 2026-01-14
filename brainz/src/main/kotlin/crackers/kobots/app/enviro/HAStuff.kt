/*
 * Copyright 2022-2026 by E. A. Graham, Jr.
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
import crackers.kobots.app.Jimmy
import crackers.kobots.mqtt.homeassistant.*
import crackers.kobots.parts.enumValue

object HAStuff : AppCommon.Startable {
    val haIdentifier = DeviceIdentifier("Kobots", "BRAINZ")

    /**
     * Actions
     */
    private val selectorHandler =
        object : KobotSelectEntity.Companion.SelectHandler {
            override val options =
                DieAufseherin.BrainzActions.entries
                    .map { it.name }
                    .sorted()

            override fun executeOption(select: String) {
                val payloadToEnum = enumValue<DieAufseherin.BrainzActions>(select.uppercase())
                DieAufseherin.actionTime(payloadToEnum)
            }
        }
    private val selector =
        object : KobotSelectEntity(selectorHandler, "brainz_selector", "Brainz", haIdentifier) {
            override fun currentState(): String = ""
        }


    // Handle the on-board NeoPixel
    private val statusLight by lazy {
        KobotRGBLight(
            "status_light",
            object : PixelBufController(Jimmy.statusPixel, offset = 0, count = 1) {
                override fun set(command: LightCommand) {
                    runCatching { super.set(command) }
                }
            },
            "Crickit Status Light",
            deviceIdentifier = haIdentifier,
        )
    }

    override fun start() {
        listOf(selector, statusLight).forEach { it.start() }
    }

    override fun stop() {
//        TODO("Not yet implemented")
    }

    internal fun updateEverything() {
        if (DieAufseherin.currentMode == DieAufseherin.SystemMode.IDLE) selector.setOption()
        listOf(selector).forEach { it.sendCurrentState() }
    }
}
