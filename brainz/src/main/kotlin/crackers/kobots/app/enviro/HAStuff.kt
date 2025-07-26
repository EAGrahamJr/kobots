/*
 * Copyright 2022-2025 by E. A. Graham, Jr.
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
import crackers.kobots.app.CannedSequences
import crackers.kobots.app.Jimmy
import crackers.kobots.app.display.DisplayDos
import crackers.kobots.mqtt.homeassistant.*
import crackers.kobots.parts.enumValue
import crackers.kobots.parts.movement.SequenceRequest

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

    private val effectList =
        mapOf(
            "cycle" to Jimmy::cycleNeo,
            "pulse" to Jimmy::pulseNeo,
        )

    /**
     * Run the 8-bit circular NeoPixel thing
     */
    private val rosetteStrand =
        KobotRGBLight(
            "crickit_rosette",
            PixelBufController(Jimmy.crickitNeoPixel, emptyMap()),
            "Crickit Neopixel",
            haIdentifier,
        )

    /**
     * Write stuff to small screen.
     */
    private val textDosEntity = KobotTextEntity(DisplayDos::text, "second_display", "Dos Display", haIdentifier)

    /**
     * Wavy thing
     */
    private val wavyHandler =
        object : KobotNumberEntity.Companion.NumberHandler {
            override fun currentState() = Jimmy.wavyThing.current.toFloat()

            override fun set(target: Float) {
                Jimmy.handleRequest(SequenceRequest(CannedSequences.setWavy(target)))
            }
        }
    private val wavyEntity =
        KobotNumberEntity(
            wavyHandler,
            "brainz_wavy",
            "Wavy Thing",
            haIdentifier,
            min = 0,
            max = 90,
            mode = KobotNumberEntity.Companion.DisplayMode.SLIDER,
            unitOfMeasurement = "deg",
        )

    val proxSensor =
        KobotAnalogSensor(
            "brainz_proximity",
            "Feline Lounge Detector",
            haIdentifier,
            KobotAnalogSensor.Companion.AnalogDevice.NONE,
        )

    override fun start() {
        listOf(selector, rosetteStrand, textDosEntity, wavyEntity, proxSensor).forEach { it.start() }
    }

    override fun stop() {
//        TODO("Not yet implemented")
    }

    internal fun updateEverything() {
        listOf(wavyEntity, selector).forEach { it.sendCurrentState() }
    }
}
