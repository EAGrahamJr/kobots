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

import crackers.kobots.app.Jimmy
import crackers.kobots.app.display.DisplayDos
import crackers.kobots.mqtt.homeassistant.*
import crackers.kobots.parts.enumValue
import crackers.kobots.parts.movement.DefaultActionSpeed
import crackers.kobots.parts.movement.LinearActuator
import crackers.kobots.parts.movement.sequence
import kotlin.math.roundToInt

object HAStuff {
    val haIdentifier = DeviceIdentifier("Kobots", "BRAINZ")

    private class PctHandler(
        val linear: LinearActuator, val thing: String, val speed: DefaultActionSpeed =
            DefaultActionSpeed.SLOW
    ) :
        KobotNumberEntity.Companion.NumberHandler {
        override fun currentState() = linear.current().toFloat()

        override fun set(target: Float) {
            val requested = sequence {
                name = "HA Move $thing"
                action {
                    requestedSpeed = speed
                    linear goTo target.roundToInt()
                }
            }
            Jimmy does requested
        }
    }

    /**
     * Actions
     */
    private val selectorHandler = object : KobotSelectEntity.Companion.SelectHandler {
        override val options = DieAufseherin.BrainzActions.entries.map { it.name }.sorted()

        override fun executeOption(select: String) {
            val payloadToEnum = enumValue<DieAufseherin.BrainzActions>(select.uppercase())
            DieAufseherin.actionTime(payloadToEnum)
        }
    }
    private val selector = KobotSelectEntity(selectorHandler, "brainz_selector", "Brainz", haIdentifier)


    /**
     * Run the 8-bit circular NeoPixel thing
     */
    private val rosetteStrand =
        KobotRGBLight("crickit_rosette", PixelBufController(Jimmy.crickitNeoPixel), "Crickit Neopixel", haIdentifier)

    /**
     * Write stuff to small screen.
     */
    val textDosEntity = KobotTextEntity(DisplayDos::text, "second_display", "Dos Display", haIdentifier)

    internal fun startDevices() {
        listOf(selector, rosetteStrand, textDosEntity).forEach { it.start() }
    }

    internal fun updateEverything() {
//        listOf(lifterEntity, swiperEntity).forEach { it.sendCurrentState() }
    }
}
