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
import com.diozero.api.DeviceMode
import com.diozero.api.DigitalInputEvent
import com.diozero.api.RuntimeIOException
import com.diozero.internal.spi.*
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.SignalMode
import java.util.concurrent.atomic.AtomicBoolean

/**
 * "Internal device" of the Crickit for the signal block. Note that changing modes of the device is allowed (although
 * not necessarily used in practice).
 */
class CRICKITSignal(crickitHat: CRICKITHat, internal val seeSawPin: Int) {
    private val seeSaw: AdafruitSeeSaw = crickitHat.seeSaw

    internal val canWrite = AtomicBoolean(false)

    // pull up vs pull down - pull-up needs to flip the value around for it to be consistent
    private val flipValue = AtomicBoolean(false)

    private lateinit var myMode: SignalMode
    var mode: SignalMode
        get() = myMode
        set(value) {
            myMode = value
            flipValue.set(mode != SignalMode.INPUT_PULLDOWN)
            canWrite.set(mode == SignalMode.OUTPUT)
            seeSaw.pinMode(seeSawPin, value)
        }

    var value: Boolean
        get() {
            if (canWrite.get()) throw RuntimeIOException("Input is not allowed on output devices.")
            return seeSaw.digitalRead(seeSawPin).let { if (flipValue.get()) !it else it }
        }
        set(value) {
            if (!canWrite.get()) throw RuntimeIOException("Output is not allowed on input devices.")
            seeSaw.digitalWrite(seeSawPin, value)
        }

    fun read(): Int = seeSaw.analogRead(seeSawPin.toByte()).toInt()
}

/**
 * "Internal device" of the Crickit for the signal block, digial (input/output) flavor.
 */
internal class SignalDigitalDevice(
    key: String,
    factory: DeviceFactoryInterface,
    private val deviceNumber: Int,
    private val delegate: CRICKITSignal
) : AbstractInputDevice<DigitalInputEvent>(key, factory),
    GpioDigitalOutputDeviceInterface, GpioDigitalInputDeviceInterface {

    override fun getGpio() = deviceNumber
    override fun getMode() = if (delegate.canWrite.get()) DeviceMode.DIGITAL_OUTPUT else DeviceMode.DIGITAL_INPUT
    override fun getValue() = delegate.value
    override fun setValue(value: Boolean) {
        delegate.value = value
    }

    override fun setDebounceTimeMillis(debounceTime: Int) {
        throw UnsupportedOperationException("Not supported")
    }
}

/**
 * "Internal device" of the Crickit for the signal block, analog flavor.
 */
internal class SignalAnalogInputDevice(
    key: String,
    factory: DeviceFactoryInterface,
    private val deviceNumber: Int,
    private val delegate: CRICKITSignal
) : AbstractInputDevice<AnalogInputEvent>(key, factory),
    AnalogInputDeviceInterface {

    override fun getAdcNumber() = deviceNumber
    override fun getValue() = delegate.read() / ANALOG_MAX
    override fun generatesEvents() = false
}
