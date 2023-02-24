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
import com.diozero.devices.motor.PwmMotor
import com.diozero.internal.PwmServoDevice
import com.diozero.internal.spi.*
import com.diozero.sbc.BoardPinInfo
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.SignalMode
import crackers.kobots.devices.expander.CRICKITHat.Companion.ANALOG_MAX
import crackers.kobots.devices.expander.CRICKITHat.Companion.DIGITAL_PINS
import crackers.kobots.devices.expander.CRICKITHat.Companion.DRIVES
import crackers.kobots.devices.expander.CRICKITHat.Companion.MOTOR1A
import crackers.kobots.devices.expander.CRICKITHat.Companion.MOTOR1B
import crackers.kobots.devices.expander.CRICKITHat.Companion.MOTOR2A
import crackers.kobots.devices.expander.CRICKITHat.Companion.MOTOR2B
import crackers.kobots.devices.expander.CRICKITHat.Companion.MOTORS
import crackers.kobots.devices.expander.CRICKITHat.Companion.SERVOS
import crackers.kobots.devices.expander.CRICKITHat.Companion.TOUCH_PAD_PINS
import crackers.kobots.devices.expander.CRICKITHatDeviceFactory.Types.*
import javax.naming.OperationNotSupportedException

/**
 * A device factory for the AdaFruit
 * [CRICKIT](https://learn.adafruit.com/adafruit-crickit-hat-for-raspberry-pi-linux-computers?view=all).
 *
 * It is **HIGHLY** recommended to use the convenience functions instead of creating devices via the builders or
 * constructors as the pin numbering gets a little odd.
 */
private const val NAME = "CRICKIT"

class CRICKITHatDeviceFactory(theHat: CRICKITHat = CRICKITHat()) :
    AbstractDeviceFactory(NAME),
    GpioDeviceFactoryInterface,
    AnalogInputDeviceFactoryInterface,
    ServoDeviceFactoryInterface,
    PwmOutputDeviceFactoryInterface {

    constructor(i2CDevice: I2CDevice = defaultI2CDevice, initReset: Boolean = true) :
        this(CRICKITHat(i2CDevice, initReset))

    private val seeSaw: AdafruitSeeSaw

    init {
        seeSaw = theHat.seeSaw
    }

    enum class Types(internal val offset: Int) {
        SIGNAL(100), TOUCH(110), SERVO(120), MOTOR(130), DRIVE(140), NEOPIXEL(150), SPEAKER(160);

        fun deviceNumber(device: Int) = offset + device
        fun indexOf(deviceId: Int) = deviceId - offset
    }

    /**
     * Convenience function to get a digital input on the Signal block [pin] (1-8), with optional [pullDown]
     */
    fun signalDigitalIn(pin: Int, pullDown: Boolean = false) =
        DigitalInputDevice(
            this,
            SIGNAL.deviceNumber(pin),
            if (pullDown) GpioPullUpDown.PULL_DOWN else GpioPullUpDown.PULL_UP,
            GpioEventTrigger.NONE
        )

    /**
     * Convenience function to get a digital output on the Signal block [pin] (1-8)
     */
    fun signalDigitalOut(pin: Int) = DigitalOutputDevice(this, SIGNAL.deviceNumber(pin), true, false)

    /**
     * Convenience function to get an analog input on the  Signal block [pin] (1-8)
     */
    fun signalAnalogIn(pin: Int) = AnalogInputDevice(this, SIGNAL.deviceNumber(pin))

    /**
     * Convenience function to get a digital input on one of the touchpads.
     */
    fun touchDigitalIn(pin: Int) = DigitalInputDevice(this, TOUCH.deviceNumber(pin))

    /**
     * Convenience function to get an analog input on one of the touchpads.
     */
    fun touchAnalogIn(pin: Int) = AnalogInputDevice(this, TOUCH.deviceNumber(pin))

    /**
     * Convenience function to get a servo device on the Servo block [pin] (1-4)
     */
    fun servo(pin: Int, servoTrim: ServoTrim = ServoTrim.DEFAULT) =
        ServoDevice.Builder.builder(SERVO.deviceNumber(pin))
            .setDeviceFactory(this)
            .setTrim(servoTrim)
            .build()

    /**
     * Convenience function to use the Motor block for bidirectional motors, where [index] is the motor _number_ (1 or
     * 2).
     */
    fun motor(index: Int): PwmMotor {
        val pins = if (index == 1) MOTOR1A to MOTOR1B else MOTOR2A to MOTOR2B
        return PwmMotor(this, pins.first, pins.second)
    }

    // -----------------------------------------------------------------------------------------------------------------
    // internal quick-access functionality
    /**
     * Get a device for the indicated touchpad (1-4)
     */
    internal fun touch(index: Int) = CRICKITTouch(seeSaw, index)

    /**
     * Get a device for the indicated signal (1-8)
     */
    internal fun signal(index: Int) = DIGITAL_PINS[index - 1].let { pin -> CRICKITSignal(seeSaw, pin) }

    // -----------------------------------------------------------------------------------------------------------------
    // diozero device and factory stuff

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
    private val boardInfo: BoardPinInfo = BoardPinInfo().apply {
        val gpioPinModes = setOf(DeviceMode.DIGITAL_INPUT, DeviceMode.DIGITAL_OUTPUT, DeviceMode.ANALOG_INPUT)
        DIGITAL_PINS.forEachIndexed { index, pin ->
            val info = crickitPinInfo(SIGNAL, index + 1, pin, gpioPinModes, ANALOG_MAX)
            addAdcPinInfo(info)
            addGpioPinInfo(info)
        }

        val touchPinModes = setOf(DeviceMode.ANALOG_INPUT, DeviceMode.DIGITAL_INPUT)
        TOUCH_PAD_PINS.forEachIndexed { index, pin ->
            val info = crickitPinInfo(TOUCH, index + 1, pin, touchPinModes, ANALOG_MAX)
            addAdcPinInfo(info)
            addGpioPinInfo(info)
        }

        val servoMode = setOf(DeviceMode.SERVO, DeviceMode.PWM_OUTPUT)
        SERVOS.forEachIndexed { index, pin ->
            val info = crickitPinInfo(SERVO, index + 1, pin, servoMode)
            addGpioPinInfo(info)
        }

        // all the motor pins
        val pwmMode = setOf(DeviceMode.PWM_OUTPUT)
        MOTORS.forEachIndexed { index, pin ->
            val pi = crickitPinInfo(MOTOR, index + 1, pin, pwmMode).apply {
                addPwmPinInfo(header, deviceNumber, name, physicalPin, MOTOR.ordinal, physicalPin, pwmMode)
            }
            addGpioPinInfo(pi)
        }
        DRIVES.forEachIndexed { index, pin ->
            val pi = crickitPinInfo(DRIVE, index + 1, pin, pwmMode).apply {
                addPwmPinInfo(header, deviceNumber, name, physicalPin, DRIVE.ordinal, physicalPin, pwmMode)
            }
            addGpioPinInfo(pi)
        }
    }

    /**
     * Create the pins for this CRICKIT
     */
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
    ): GpioDigitalInputDeviceInterface = pinInfo.deviceNumber.let { deviceNum ->
        if (pinInfo.keyPrefix == SIGNAL.name) {
            val signal = signal(SIGNAL.indexOf(deviceNum)).apply {
                mode = when (pud) {
                    GpioPullUpDown.NONE -> SignalMode.INPUT
                    GpioPullUpDown.PULL_UP -> SignalMode.INPUT_PULLUP
                    GpioPullUpDown.PULL_DOWN -> SignalMode.INPUT_PULLDOWN
                }
            }
            SignalDigitalDevice(key, this, deviceNum, signal)
        } else {
            val sensor = touch(TOUCH.indexOf(deviceNum))
            DigitalTouch(key, this, deviceNum, sensor)
        }
    }

    override fun createDigitalOutputDevice(
        key: String,
        pinInfo: PinInfo,
        initialValue: Boolean
    ): GpioDigitalOutputDeviceInterface = pinInfo.deviceNumber.let { deviceNum ->
        val signal = signal(SIGNAL.indexOf(deviceNum)).apply {
            mode = SignalMode.OUTPUT
        }
        SignalDigitalDevice(key, this, deviceNum, signal)
    }

    override fun createDigitalInputOutputDevice(
        key: String,
        pinInfo: PinInfo,
        mode: DeviceMode
    ): GpioDigitalInputOutputDeviceInterface {
        throw RuntimeIOException("Device must be either input or output (not both).")
    }

    override fun createAnalogInputDevice(key: String, pinInfo: PinInfo): AnalogInputDeviceInterface =
        pinInfo.deviceNumber.let { deviceNum ->
            if (pinInfo.keyPrefix == SIGNAL.name) {
                val signal = signal(SIGNAL.indexOf(deviceNum)).apply {
                    mode = SignalMode.INPUT
                }
                SignalAnalogInputDevice(key, this, deviceNum, signal)
            } else {
                val sensor = touch(TOUCH.indexOf(deviceNum))
                AnalogTouch(key, this, deviceNum, sensor)
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
    ): InternalServoDeviceInterface {
        val pwm = pinInfo.let {
            CRICKITnternalPwm(key, it.deviceNumber, seeSaw, it.physicalPin, frequencyHz)
        }
        return PwmServoDevice(key, this, pwm, minPulseWidthUs, maxPulseWidthUs, initialPulseWidthUs)
    }

    override fun getBoardPwmFrequency() = 50

    override fun setBoardPwmFrequency(pwmFrequency: Int) {
        throw OperationNotSupportedException("Cannot set board frequency")
    }

    override fun createPwmOutputDevice(
        key: String,
        pinInfo: PinInfo,
        pwmFrequency: Int,
        initialValue: Float
    ): InternalPwmOutputDeviceInterface = pinInfo.let {
        CRICKITnternalPwm(key, it.deviceNumber, seeSaw, it.physicalPin, 50).apply {
            value = initialValue
        }
    }
}
