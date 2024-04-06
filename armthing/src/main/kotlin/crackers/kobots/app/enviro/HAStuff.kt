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


import crackers.kobots.app.crickitHat
import crackers.kobots.app.display.DisplayDos
import crackers.kobots.app.enviro.DieAufseherin.currentMode
import crackers.kobots.mqtt.homeassistant.*
import crackers.kobots.parts.enumValue

object HAStuff {
    val haIdentifier = DeviceIdentifier("Kobots", "CRICKIT")

    private val noodle by lazy { crickitHat.signalDigitalOut(8) }
    private var noodValue = false

    /**
     * Nood attached to crickit hat.
     */
    private var nood: Boolean
        get() = noodValue
        set(value) {
            noodValue = value
            noodle.setValue(value)
        }

    val noodSwitch: KobotSwitch = KobotSwitch(
        object : KobotSwitch.Companion.OnOffDevice {
            override var isOn: Boolean
                get() = nood
                set(v) {
                    nood = v
                }
            override val name = "CrickitHat 8"
        },
        "crickit_nood",
        "Crickit Hat Nood",
        deviceIdentifier = haIdentifier
    )

    /**
     * Actions
     */
    private val selectorHandler = object : KobotSelectEntity.Companion.SelectHandler {
        override val options = DieAufseherin.GripperActions.entries.map { it.name }.sorted()

        override fun executeOption(select: String) {
            val payloadToEnum = enumValue<DieAufseherin.GripperActions>(select.uppercase())
            DieAufseherin.actionTime(payloadToEnum)
        }
    }
    val selector = object : KobotSelectEntity(
        selectorHandler, "arm_thing", "Arm Thing", deviceIdentifier =
        haIdentifier
    ) {
        override fun sendCurrentState(state: String) {
            if (currentMode != DieAufseherin.SystemMode.IDLE) super.sendCurrentState(currentState())
            else super.sendCurrentState("None")
        }
    }

    /**
     * Run the 8-bit circular NeoPixel thing
     */
    val crickitNeoPixel = crickitHat.neoPixel(8).apply { brightness = .005f }
    private val rosetteController = PixelBufController(crickitNeoPixel)
    val rosetteStrand = KobotRGBLight("crickit_rosette", rosetteController, "Crickit Neopixel", haIdentifier)

    /**
     * Write stuff to small screen.
     */
    val textDosEntity = KobotTextEntity(DisplayDos::text, "second_display", "Dos Display", haIdentifier)


    internal fun startDevices() {
        // HA stuff
        noodSwitch.start()
        selector.start()
        rosetteStrand.start()
        textDosEntity.start()
    }
}
