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
import com.diozero.internal.spi.*
import com.diozero.sbc.BoardPinInfo
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.SignalMode
import crackers.kobots.devices.expander.CRICKITHat.Companion.ANALOG_MAX
import crackers.kobots.devices.expander.CRICKITHat.Companion.DIGITAL_PINS
import crackers.kobots.devices.expander.CRICKITHat.Companion.SERVOS
import crackers.kobots.devices.expander.CRICKITHat.Companion.TOUCH_PAD_PINS

/**
 * https://learn.adafruit.com/adafruit-crickit-hat-for-raspberry-pi-linux-computers?view=all
 */
private const val NAME = "CRICKIT"

class CRICKITHatDeviceFactory(private val theHat: CRICKITHat) :
    AbstractDeviceFactory(NAME),
    GpioDeviceFactoryInterface,
    AnalogInputDeviceFactoryInterface,
    ServoDeviceFactoryInterface,
    AnalogOutputDeviceFactoryInterface {

    constructor(i2CDevice: I2CDevice = defaultI2CDevice, initReset: Boolean = true) :
            this(CRICKITHat(i2CDevice, initReset))

    enum class Types(internal val offset: Int) {
        SIGNAL(0), TOUCH(10), SERVO(20), MOTOR(30), HI_CURRENT(40), NEOPIXEL(50), SPEAKER(60);

        fun deviceNumber(device: Int) = offset + device
        fun indexOf(deviceId: Int) = deviceId - offset
    }

    /**
     * Create the `diozero` "board info" for this expander.
     *
     * - `header`: the "bus" the device is tied to
     * - `pin`: the Crickit offset as defined in the companion
     * - `deviceId`: the "type" offset (10, 20) plus the device number (1,2,3)
     *   - this is to distinguish creating conflicting devices (e.g. Touch and Signal both have digital inputs)
     * - `name`: header plus (device id - offset)
     */
    private val boardInfo: BoardPinInfo = BoardPinInfo().apply {
        val gpioPinModes = setOf(DeviceMode.DIGITAL_INPUT, DeviceMode.DIGITAL_OUTPUT, DeviceMode.ANALOG_INPUT)
        DIGITAL_PINS.forEachIndexed { index, pin ->
            val info = crickitPinInfo(Types.SIGNAL, index + 1, pin, gpioPinModes, ANALOG_MAX)
            addAdcPinInfo(info)
            addGpioPinInfo(info)
        }

        val touchPinModes = setOf(DeviceMode.ANALOG_INPUT, DeviceMode.DIGITAL_INPUT)
        TOUCH_PAD_PINS.forEachIndexed { index, pin ->
            val info = crickitPinInfo(Types.TOUCH, index + 1, pin, touchPinModes, ANALOG_MAX)
            addAdcPinInfo(info)
            addGpioPinInfo(info)
        }

        val servoMode = setOf(DeviceMode.SERVO)
        SERVOS.forEachIndexed { index, pin ->
            val info = crickitPinInfo(Types.SERVO, index + 1, pin, servoMode)
            addGpioPinInfo(info)
        }
    }


    override fun close() {
        theHat.close()
    }

    override fun getName() = NAME

    private fun crickitPinInfo(
        type: Types,
        deviceId: Int,
        pin: Int,
        pinModes: Collection<DeviceMode>,
        vRef: Float = -1f
    ) = PinInfo(type.name, getName(), type.deviceNumber(deviceId), pin, "$type-$deviceId", pinModes, vRef)

    override fun getBoardPinInfo() = boardInfo

    override fun createDigitalInputDevice(
        key: String,
        pinInfo: PinInfo,
        pud: GpioPullUpDown,
        trigger: GpioEventTrigger
    ): GpioDigitalInputDeviceInterface = pinInfo.deviceNumber.let { deviceId ->
        if (pinInfo.keyPrefix == Types.SIGNAL.name) {
            val signal = theHat.signal(Types.SIGNAL.indexOf(deviceId)).apply {
                mode = when (pud) {
                    GpioPullUpDown.NONE -> SignalMode.INPUT
                    GpioPullUpDown.PULL_UP -> SignalMode.INPUT_PULLUP
                    GpioPullUpDown.PULL_DOWN -> SignalMode.INPUT_PULLDOWN
                }
            }
            SignalDigitalDevice(key, this, deviceId, signal)
        } else {
            val sensor = theHat.touch(Types.TOUCH.indexOf(deviceId))
            DigitalTouch(key, this, deviceId, sensor)
        }
    }

    override fun createDigitalOutputDevice(
        key: String,
        pinInfo: PinInfo,
        initialValue: Boolean
    ): GpioDigitalOutputDeviceInterface = pinInfo.deviceNumber.let { deviceId ->
        val signal = theHat.signal(Types.SIGNAL.indexOf(pinInfo.deviceNumber)).apply {
            mode = SignalMode.OUTPUT
        }
        SignalDigitalDevice(key, this, pinInfo.deviceNumber, signal)
    }

    override fun createDigitalInputOutputDevice(
        key: String,
        pinInfo: PinInfo,
        mode: DeviceMode
    ): GpioDigitalInputOutputDeviceInterface {
        throw RuntimeIOException("Device must be either input or output (not both).")
    }

    override fun createAnalogInputDevice(key: String, pinInfo: PinInfo): AnalogInputDeviceInterface =
        pinInfo.deviceNumber.let { deviceId ->
            if (pinInfo.keyPrefix == Types.SIGNAL.name) {
                val signal = theHat.signal(Types.SIGNAL.indexOf(deviceId)).apply {
                    mode = SignalMode.INPUT
                }
                SignalAnalogInputDevice(key, this, deviceId, signal)
            } else {
                val sensor = theHat.touch(Types.TOUCH.indexOf(deviceId))
                AnalogTouch(key, this, deviceId, sensor)
            }
        }

    // typical
    override fun getBoardServoFrequency() = 50
    override fun setBoardServoFrequency(servoFrequency: Int) {
        throw UnsupportedOperationException("Board does not support setting 'default' frequencies")
    }

    override fun createServoDevice(
        key: String,
        pinInfo: PinInfo,
        frequencyHz: Int,
        minPulseWidthUs: Int,
        maxPulseWidthUs: Int,
        initialPulseWidthUs: Int
    ): InternalServoDeviceInterface = pinInfo.physicalPin.let { seeSawPin ->
        InternalServo(
            key,
            pinInfo.deviceNumber,
            theHat.seeSaw,
            seeSawPin,
            frequencyHz,
            minPulseWidthUs,
            maxPulseWidthUs,
            initialPulseWidthUs
        )
    }

    override fun createAnalogOutputDevice(
        key: String?,
        pinInfo: PinInfo?,
        initialValue: Float
    ): AnalogOutputDeviceInterface {
        TODO("Not yet implemented")
    }
}
