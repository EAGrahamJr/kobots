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

import com.diozero.api.*
import com.diozero.api.function.DeviceEventConsumer
import com.diozero.internal.spi.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * "Internal device" of the Crickit for the signal block. Note that changing modes of the device is allowed (although
 * not necessarily used in practice).
 */
class SignalDigitalDevice(key: String, crickitHat: CRICKITHat, info: PinInfo) :
    AbstractDevice(key, crickitHat),
    GpioDigitalOutputDeviceInterface,
    GpioDigitalInputDeviceInterface {

    private val canWrite = AtomicBoolean(false)
    private val deviceNumber = info.deviceNumber
    private val seeSawPin: Int = info.physicalPin
    private val seeSaw: AdafruitSeeSaw = crickitHat.seeSaw

    // pull up vs pull down - pull-up needs to flip the value around?
    private val flipValue = AtomicBoolean(false)

    override fun getGpio() = deviceNumber
    override fun getMode(): DeviceMode = if (canWrite.get()) DeviceMode.DIGITAL_OUTPUT else DeviceMode.DIGITAL_INPUT

    override fun getValue(): Boolean {
        if (canWrite.get()) throw RuntimeIOException("Input is not allowed on output devices.")
        return seeSaw.digitalRead(seeSawPin).let { if (flipValue.get()) !it else it }
    }

    override fun setDebounceTimeMillis(debounceTime: Int) {
        throw UnsupportedOperationException("Not supported")
    }

    override fun setValue(value: Boolean) {
        if (!canWrite.get()) throw RuntimeIOException("Output is not allowed on input devices.")
        seeSaw.digitalWrite(seeSawPin, value)
    }

    fun setInputMode(pud: GpioPullUpDown) {
        flipValue.set(pud != GpioPullUpDown.PULL_DOWN)

        seeSaw.pinMode(
            seeSawPin,
            when (pud) {
                GpioPullUpDown.NONE -> AdafruitSeeSaw.Companion.SignalMode.INPUT
                GpioPullUpDown.PULL_UP -> AdafruitSeeSaw.Companion.SignalMode.INPUT_PULLUP
                GpioPullUpDown.PULL_DOWN -> AdafruitSeeSaw.Companion.SignalMode.INPUT_PULLDOWN
            }
        )
        canWrite.set(false)
    }

    fun setOutputMode() {
        seeSaw.pinMode(seeSawPin, AdafruitSeeSaw.Companion.SignalMode.OUTPUT)
        canWrite.set(true)
    }

    override fun setListener(listener: DeviceEventConsumer<DigitalInputEvent>?) {
        TODO("Not yet implemented")
    }

    override fun removeListener() {
        TODO("Not yet implemented")
    }

    override fun closeDevice() {
//        removeListener()
    }
}

/**
 * "Internal device" of the Crickit for the signal block, analog flavor.
 */
class SignalAnalogInputDevice(private val crickitHat: CRICKITHat, val info: PinInfo) : AnalogInputDeviceInterface,
    AbstractInputDevice<AnalogInputEvent>(crickitHat.createPinKey(info), crickitHat) {
    private val seeSawPin: Int = info.physicalPin

    fun raw() = crickitHat.seeSaw.analogRead(seeSawPin.toByte())

    override fun getAdcNumber() = info.deviceNumber

    override fun getValue() = raw() / ANALOG_MAX

    override fun generatesEvents() = false
}
