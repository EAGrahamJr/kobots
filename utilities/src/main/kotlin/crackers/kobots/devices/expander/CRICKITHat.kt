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

/**
 * https://learn.adafruit.com/adafruit-crickit-hat-for-raspberry-pi-linux-computers?view=all
 */
private const val NAME = "CRICKIT"
private val defaultI2CDevice by lazy { I2CDevice(1, CRICKITHat.DEVICE_ADDRESS) }

class CRICKITHat(i2CDevice: I2CDevice = defaultI2CDevice, initReset: Boolean = true) :
    AbstractDeviceFactory(NAME),
    GpioDeviceFactoryInterface,
    AnalogInputDeviceFactoryInterface {

    enum class Types(internal val offset: Int) {
        SIGNAL(0), TOUCH(10), SERVO(20), MOTOR(30), HI_CURRENT(40), NEOPIXEL(50), SPEAKER(60);

        fun deviceNumber(device: Int) = offset + device
    }

    val seeSaw = AdafruitSeeSaw(i2CDevice, initReset = initReset).apply {
        analogInputPins = digitalPins
    }

    override fun close() {
        try {
            seeSaw.close()
        } catch (_: Throwable) {
        }
    }

    override fun getName() = NAME

    /**
     * Create the `diozero` "board info" for this expander.
     *
     * - `header`: the "bus" the device is tied to
     * - `pin`: the Crickit offset as defined in the companion
     * - `deviceId`: the "type" offset (10, 20) plus the device number (1,2,3)
     *   - this is to distinguish creating conflicting devices (e.g. Touch and Signal both have digital inputs)
     * - `name`: header plus (device id - offset)
     */
    private val boardInfo = BoardPinInfo().apply {
        val gpioPinModes = setOf(DeviceMode.DIGITAL_INPUT, DeviceMode.DIGITAL_OUTPUT, DeviceMode.ANALOG_INPUT)
        digitalPins.forEachIndexed { index, pin ->
            val info = crickitPinInfo(Types.SIGNAL, index + 1, pin, gpioPinModes)
            addAdcPinInfo(info)
            addGpioPinInfo(info)
        }

        val touchPinModes = setOf(DeviceMode.ANALOG_INPUT, DeviceMode.DIGITAL_INPUT)
        touchpadPins.forEachIndexed { index, pin ->
            val info = crickitPinInfo(Types.TOUCH, index + 1, pin, touchPinModes, ANALOG_MAX)
            addAdcPinInfo(info)
            addGpioPinInfo(info)
        }
    }

    private fun crickitPinInfo(
        type: Types,
        deviceId: Int,
        pin: Int,
        pinModes: Collection<DeviceMode>,
        vRef: Float = -1f
    ) = PinInfo(type.name, getName(), type.deviceNumber(deviceId), pin, "$type-$deviceId", pinModes, vRef)

    override fun getBoardPinInfo() = boardInfo

    /**
     * Get a device for the indicated touchpad (1-4)
     */
    fun touch(index: Int) = CRICKITTouch(this, index)

    fun signal(index: Int) = CRICKITSignal(this, digitalPins[index - 1])

    // diozero implementations ----------------------------------------------------------------------------------------
    override fun createDigitalInputDevice(
        key: String,
        pinInfo: PinInfo,
        pud: GpioPullUpDown,
        trigger: GpioEventTrigger
    ): GpioDigitalInputDeviceInterface =
        if (pinInfo.keyPrefix == Types.SIGNAL.name) {
            val signal = CRICKITSignal(this, pinInfo.physicalPin).apply {
                mode = when (pud) {
                    GpioPullUpDown.NONE -> SignalMode.INPUT
                    GpioPullUpDown.PULL_UP -> SignalMode.INPUT_PULLUP
                    GpioPullUpDown.PULL_DOWN -> SignalMode.INPUT_PULLDOWN
                }
            }
            SignalDigitalDevice(key, this, pinInfo.deviceNumber, signal)
        } else {
            val sensor = CRICKITTouch(this, pinInfo.deviceNumber - Types.TOUCH.offset)
            DigitalTouch(key, this, pinInfo.deviceNumber, sensor)
        }

    override fun createDigitalOutputDevice(
        key: String,
        pinInfo: PinInfo,
        initialValue: Boolean
    ): GpioDigitalOutputDeviceInterface {
        val signal = CRICKITSignal(this, pinInfo.physicalPin).apply {
            mode = SignalMode.OUTPUT
        }
        return SignalDigitalDevice(key, this, pinInfo.deviceNumber, signal)
    }

    override fun createDigitalInputOutputDevice(
        key: String,
        pinInfo: PinInfo,
        mode: DeviceMode
    ): GpioDigitalInputOutputDeviceInterface {
        throw RuntimeIOException("Device must be either input or output (not both).")
    }

    override fun createAnalogInputDevice(key: String, pinInfo: PinInfo): AnalogInputDeviceInterface =
        pinInfo.deviceNumber.let { index ->
            if (pinInfo.keyPrefix == Types.SIGNAL.name) {
                val signal = CRICKITSignal(this, pinInfo.physicalPin).apply {
                    mode = SignalMode.INPUT
                }
                SignalAnalogInputDevice(key, this, pinInfo.deviceNumber, signal)
            } else {
                val sensor = touch(pinInfo.deviceNumber - Types.TOUCH.offset)
                AnalogTouch(key, this, pinInfo.deviceNumber, sensor)
            }
        }

    fun createServoDevice(
        key: String?,
        pinInfo: PinInfo?,
        frequencyHz: Int,
        minPulseWidthUs: Int,
        maxPulseWidthUs: Int,
        initialPulseWidthUs: Int
    ): InternalServoDeviceInterface {
        TODO("Not yet implemented")
    }

    companion object {
        const val DEVICE_ADDRESS = 0x49

        const val ANALOG_MAX = 1023f

        const val CAPTOUCH1 = 4
        const val CAPTOUCH2 = 5
        const val CAPTOUCH3 = 6
        const val CAPTOUCH4 = 7
        val touchpadPins = intArrayOf(CAPTOUCH1, CAPTOUCH2, CAPTOUCH3, CAPTOUCH4)

        const val SIGNAL1 = 2
        const val SIGNAL2 = 3
        const val SIGNAL3 = 40
        const val SIGNAL4 = 41
        const val SIGNAL5 = 11
        const val SIGNAL6 = 10
        const val SIGNAL7 = 9
        const val SIGNAL8 = 8
        val digitalPins = intArrayOf(SIGNAL1, SIGNAL2, SIGNAL3, SIGNAL4, SIGNAL5, SIGNAL6, SIGNAL7, SIGNAL8)

        const val SERVO4 = 14
        const val SERVO3 = 15
        const val SERVO2 = 16
        const val SERVO1 = 17

        const val MOTOR2B = 18
        const val MOTOR2A = 19
        const val MOTOR1A = 22
        const val MOTOR1B = 23
        const val DRIVE4 = 42
        const val DRIVE3 = 43
        const val DRIVE2 = 12
        const val DRIVE1 = 13
    }
}
