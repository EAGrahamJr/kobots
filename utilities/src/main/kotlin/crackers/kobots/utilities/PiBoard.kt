/*
 * Copyright 2022-2022 by E. A. Graham, Jr.
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

package crackers.kobots.utilities

import com.diozero.api.PinInfo
import com.diozero.internal.spi.NativeDeviceFactoryInterface
import com.diozero.sbc.BoardPinInfo
import com.diozero.sbc.DeviceFactoryHelper

/**
 * Probably specific to the Raspberry Pi.
 */
const val PI_HEADER = "J8"

/**
 * Locate `PinInfo` for a selected board by name and header (defaults to Pi).
 */
fun BoardPinInfo.byName(name: String, header: String = PI_HEADER) =
    headers[header]?.values?.first {
        it.name == name
    } ?: throw NoSuchElementException("Header '$name' not found")

/**
 * Default device factory.
 */
val nativeDeviceFactory: NativeDeviceFactoryInterface by lazy { DeviceFactoryHelper.getNativeDeviceFactory() }

/**
 * Default board pin info.
 */
val defaultPins: BoardPinInfo by lazy { nativeDeviceFactory.getBoardPinInfo() }

/**
 * Pin information for all the Raspbarry Pi pins.
 */
val piPins: Collection<PinInfo>? by lazy { defaultPins.headers[PI_HEADER]?.values }

/**
 * Pretty-print the pin names, device number, and physical pin.
 */
fun `pretty print pins`() {
    val columnWidth = piPins!!.maxOf { it.name.length }
    val template = "| %-${columnWidth}s | %3s | %3d |"
    println("| %${columnWidth}s | Num | Pin |".format("Name"))
    println("|${"-".repeat(columnWidth + 2)}|-----|-----|")
    piPins!!.forEach {
        println(template.format(it.name, if (it.deviceNumber == -1) "" else "%3d".format(it.deviceNumber), it.physicalPin))
    }
}
