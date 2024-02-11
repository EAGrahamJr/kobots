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
import crackers.kobots.mqtt.homeassistant.DeviceIdentifier
import crackers.kobots.mqtt.homeassistant.KobotSelect
import crackers.kobots.mqtt.homeassistant.KobotSwitch
import crackers.kobots.parts.enumValue

val haIdentifier = DeviceIdentifier("Kobots", "CRICKIT", "mdi:list")

private val noodle by lazy { crickitHat.signalDigitalOut(8) }
private var noodValue = false

/**
 * Nood attached to crickit hat.
 */
var nood: Boolean
    get() = noodValue
    set(value) {
        noodValue = value
        noodle.setValue(value)
    }

val noodSwitch: KobotSwitch = KobotSwitch(object : KobotSwitch.Companion.OnOffDevice {
    override var isOn: Boolean
        get() = nood
        set(v) {
            nood = v
        }
    override val name = "CrickitHat 8"
}, "crickit_nood", "Crickit Hat Nood", deviceIdentifier = haIdentifier)

private val selectorHandler = object : KobotSelect.Companion.SelectHandler {
    override val options: List<String> = DieAufseherin.GripperActions.entries.map { it.name }.sorted()

    override fun executeOption(select: String) {
        val payloadToEnum = enumValue<DieAufseherin.GripperActions>(select.uppercase())
        DieAufseherin.actionTime(payloadToEnum)
    }
}
val selector = KobotSelect(selectorHandler, "arm_thing", "Arm Thing", deviceIdentifier = haIdentifier)
