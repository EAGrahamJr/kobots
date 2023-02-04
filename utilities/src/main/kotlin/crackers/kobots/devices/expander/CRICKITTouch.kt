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

package crackers.kobots.devices.expander

import com.diozero.api.AnalogInputEvent
import com.diozero.api.DigitalInputEvent
import com.diozero.internal.spi.AbstractInputDevice
import com.diozero.internal.spi.AnalogInputDeviceInterface
import com.diozero.internal.spi.DeviceFactoryInterface
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface
import crackers.kobots.devices.expander.CRICKITHat.Companion.ANALOG_MAX

/**
 * "Internal device" of the Crickit for the capacitive touch sensors.
 *
 * Because there's always a value present, the initial `read()` function will max the next 5 values and
 * assume any value greater than that (+100) is a "touch" for basic true/false functions.
 */
class CRICKITTouch(crickitHat: CRICKITHat, padNumber: Int) {
    private val seesawPadIndex = padNumber - 1
    private val seeSaw: AdafruitSeeSaw = crickitHat.seeSaw

    private val noTouchThreshold: Int by lazy {
        ((1..5).map { value }.max() + 100)
    }

    val value: Int
        get() = seeSaw.touchRead(seesawPadIndex)
    val isOn: Boolean
        get() = noTouchThreshold < value
}

// diozero bridge device
internal class DigitalTouch(
    key: String,
    factory: DeviceFactoryInterface,
    private val io: Int,
    private val sensor: CRICKITTouch
) :
    AbstractInputDevice<DigitalInputEvent>(key, factory), GpioDigitalInputDeviceInterface {

    override fun getGpio() = io

    override fun setDebounceTimeMillis(debounceTime: Int) {
        throw UnsupportedOperationException("Not applicable")
    }

    override fun getValue() = sensor.isOn
}

// diozero bridge device
internal class AnalogTouch(
    key: String,
    factory: DeviceFactoryInterface,
    private val adc: Int,
    private val sensor: CRICKITTouch
) : AbstractInputDevice<AnalogInputEvent>(key, factory), AnalogInputDeviceInterface {

    override fun getAdcNumber() = adc
    override fun getValue() = sensor.value / ANALOG_MAX
    override fun generatesEvents() = false
}
