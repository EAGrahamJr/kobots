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
package crackers.kobots.devices.sensors

import com.diozero.api.I2CDevice
import com.diozero.api.NoSuchDeviceException
import com.diozero.devices.LuminositySensorInterface
import com.diozero.util.Hex
import crackers.kobots.devices.shortSubRegister
import java.util.concurrent.atomic.AtomicInteger

/**
 * Proximity and ambient light sensor on I2C bus.
 *
 *
 * Datasheet: https://www.vishay.com/docs/84274/vcnl4040.pdf
 * [Adafruit guide](https://learn.adafruit.com/adafruit-vcnl4040-proximity-sensor?view=all)
 */
class VCNL4040 @JvmOverloads constructor(controller: Int, address: Int = ADDRESS) : LuminositySensorInterface {
    // Ambient light sensor integration times - ordinal matches data
    enum class AmbientIntegration {
        ALS_80MS, ALS_160MS, ALS_320MS, ALS_640MS
    }

    // Proximity sensor integration times - ordinal matches data
    enum class ProximityIntegration {
        PS_1T, PS_1_5T, PS_2T, PS_2_5T, PS_3T, PS_3_5T, PS_4T, PS_8T
    }

    // LED current settings - ordinal matches data
    enum class LEDCurrent {
        LED_50MA, LED_75MA, LED_100MA, LED_120MA, LED_140MA, LED_160MA, LED_180MA, LED_200MA
    }

    // LED duty cycle settings - ordinal matches data
    enum class LEDDutyCycle {
        LED_1_40, LED_1_80, LED_1_160, LED_1_320
    }

    // Proximity sensor interrupt enable/disable options
    enum class ProximityType {
        PS_INT_DISABLE, PS_INT_CLOSE, PS_INT_AWAY, PS_INT_CLOSE_AWAY
    }

    enum class ProximityResolution {
        PS_RES_12, PS_RES_16
    }

    enum class ProximityOperation {
        PS_MS_NORMAL, PS_MS_OUTPUT
    }

    private val delegate: I2CDevice

    init {
        delegate = I2CDevice(controller, address)
        val deviceAddress = delegate.readWordData(ID_REGISTER)
        if (deviceAddress.toInt() != DEVICE_ID) {
            delegate.close()
            throw NoSuchDeviceException(String.format("Chip ID did not match at this address: %04x", deviceAddress))
        }

        // TODO enable things - currently just print out registers

    }

    private val ambientIntegrationSubRegister by lazy { delegate.shortSubRegister(ALS_CONFIG, ALS_IT) }

    fun dumpRegisters() {
        (0..0x0c).forEach {
            println("Register ${Hex.encode(it)} - ${Hex.encode(delegate.readWordData(it))}")
        }
    }

    override fun close() {
        delegate.close()
    }

    override fun getLuminosity(): Float {
        // scale the lux depending on the value of the integration time
        return (ambientLight * (0.1 / (1 shl ambientIntegrationTime.ordinal))).toFloat()
    }

    val proximity: Short
        get() = delegate.readWordData(PS_DATA)

    val ambientLight: Short
        get() = delegate.readWordData(ALS_DATA)

    val whiteLight: Short
        get() {
            val wh = delegate.readWordData(WHITE_DATA)
            // scale the light depending on the value of the integration time
            return Math.round(wh * (0.1 / (1 shl ambientIntegrationValue.get()))).toShort()
        }

    fun enableProximity(enable: Boolean) {
        writeBit(PS_CONFIG12, PS_SD, enable)
    }

    fun enableAmbientLight(enable: Boolean) {
        writeBit(ALS_CONFIG, ALS_SD, enable)
    }

    fun enableWhiteLight(enable: Boolean) {
        writeBit(PS_MS_H, WHITE_EN, enable)
    }

    // cache this
    private val ambientIntegrationValue: AtomicInteger by lazy {
        AtomicInteger(ambientIntegrationSubRegister.read().toInt())
    }

    var ambientIntegrationTime: AmbientIntegration
        get() {
            return AmbientIntegration.values()[ambientIntegrationValue.get()]
        }
        set(integrationTime) {
            val newOrdinal = integrationTime.ordinal
            ambientIntegrationSubRegister.write(newOrdinal.toShort())
            ambientIntegrationValue.set(newOrdinal)
        }

    private fun writeBit(register: Int, mask: Int, enable: Boolean) {
        // read the register and flip the indicated bit
        val output = (if (enable) 0 else 1).toShort()
        delegate.shortSubRegister(register, mask).write(output)
    }


    /*

    val interruptStatus: Byte
        get() {
            val status = delegate.readWordData(INT_FLAG)
            printRegister("INT_FLAG", status)
            val bw = BitWrapper(status, 8, 8)
            return bw.read().toByte()
        }

    fun enableAmbientLightInterrupts(enable: Boolean) {
        // read the register and flip the "enable" bit
        val current = delegate.readWordData(ALS_CONFIG)
        printRegister("ALS_CONF1G", current)
        val bw = BitWrapper(current, 1, 1)
    }

    var ambientLightHighThreshold: Short
        get() = delegate.readWordData(ALS_THDH)
        set(highThreshold) {
            delegate.writeWordData(ALS_THDH, highThreshold)
        }
    var ambientLightLowThreshold: Short
        get() = delegate.readWordData(ALS_THDL)
        set(lowThreshold) {
            delegate.writeWordData(ALS_THDL, lowThreshold)
        }

    fun enableProximityInterrupts(interruptCondition: ProximityType?) {
        val current = delegate.readWordData(PS_CONF1G)
    }

    var proximityLowThreshold: Short
        get() = delegate.readWordData(PS_THDL)
        set(lowThreshold) {
            delegate.writeWordData(PS_THDL, lowThreshold)
        }
    var proximityHighThreshold: Short
        get() = delegate.readWordData(PS_THDH)
        set(highThreshold) {
            delegate.writeWordData(PS_THDH, highThreshold)
        }
    var proximityIntegrationTime: ProximityIntegration?
        get() {
            val current = delegate.readWordData(PS_CONF1G)
            printRegister("PS_CONF1G", current)
            return null
        }
        set(integrationTime) {
            val current = delegate.readWordData(PS_CONF1G)
        }
    var proximityLEDCurrent: LEDCurrent?
        get() {
            val current = delegate.readWordData(PS_MS_H)
            printRegister("PS_MS_H", current)
            return null
        }
        set(ledCurrent) {
            val current = delegate.readWordData(PS_MS_H)
        }
*/
    var proximityLEDDutyCycle: LEDDutyCycle
        get() {
            val current = delegate.shortSubRegister(PS_CONFIG12, PS_DUTY).read().toInt()
            return LEDDutyCycle.values()[current]
        }
        set(dutyCycle) {
            delegate.shortSubRegister(PS_CONFIG12, PS_DUTY).write(dutyCycle.ordinal.toShort())
        }

    var proximityHighResolution: ProximityResolution
        get() {
            val current = delegate.shortSubRegister(PS_CONFIG12, PS_HD).read().toInt()
            return ProximityResolution.values()[current]
        }
        set(highResolution) {
            delegate.shortSubRegister(PS_CONFIG12, PS_HD).write(highResolution.ordinal.toShort())
        }

    companion object {
        const val ADDRESS = 0x60
        const val DEVICE_ID = 0x0186

        // the device uses 16-bit registers
        const val ALS_CONFIG = 0x00     // Ambient light sensor configuration register
        const val ALS_THDH = 0x01       // Ambient light high threshold register
        const val ALS_THDL = 0x02       // Ambient light low threshold register
        const val PS_CONFIG12 = 0x03    // Proximity sensor configuration 1/2 register
        const val PS_CONFIG3 = 0x04     // Proximity sensor configuration 3 register (low byte)
        const val PS_MS_H = 0x04        // Proximity sensor misc config register (high byte)
        const val PS_THDL = 0x06        // Proximity sensor low threshold register
        const val PS_THDH = 0x07        // Proximity sensor high threshold register
        const val PS_DATA = 0x08        // Proximity sensor data register
        const val ALS_DATA = 0x09       // Ambient light sensor data register
        const val WHITE_DATA = 0x0A     // White light sensor data register
        const val INT_FLAG = 0x0B       // Interrupt status register
        const val ID_REGISTER = 0x0C    // Device ID

        // ALS_CONF bits - this is the lower byte of the word: the high-byte is reserved
        const val ALS_IT = 0x00C0         // integration time (2-bits - AmbientIntegration)

        // RESERVED 0b00110000
        const val ALS_PERS = 0x000C       // interrupt persistence (2-bits)
        const val ALS_INT_EN = 0x0002     // interrupt enable/disable (1 == enable)
        const val ALS_SD = 0x0001         // als power (0 == enable)

        // PS_CONF1 bits - this is the lower byte of the PS_CONFIG word
        const val PS_DUTY = 0x00C0        // duty ration setting (2-bits - LEDDutyCycle)
        const val PS_PERS = 0x0030        // interrupt persistence (2-bits)
        const val PS_IT = 0x000E          // integration time (2-bits - ProximityIntegration)
        const val PS_SD = 0x0001          // ps power (0 == enable)

        // PS_CONF2 bits - this is the upper byte of the PS_CONFIG word
        // RESERVED 0b11000000
        // RESERVED 0b00110000
        const val PS_HD = 0x0800          // ps resolution (ProximityResolution)

        // RESERVED 0b00000100
        const val PS_INT = 0x0300         // ps interrupt type (2-bits - ProximityType)

        // PS_CONF3 bits - this is the lower byte of the PS_CONF3/PS_MS_H register
        // RESERVED 0b1000000
        const val PS_MPS = 0x0060         // proximity multi-pulse numbers
        const val PS_SMART_PERS = 0x0010  // smart persistence
        const val PS_AF = 0x0008          // active force mode (normally 0)
        const val PS_TRIG = 0x0004        // trigger output cycle

        // RESERVED 0x02
        const val PS_SC_EN = 0x0001       // sunlight cancellation function

        // PS_MS_H bits - this is the upper byte of the PS_CONF3/PS_MS_H register
        const val WHITE_EN = 0x8000       // white channel enabled (0 == enabled)
        const val PS_MS = 0x4000          // proximity function (ProximityOperation)

        // RESERVED 0b00111000
        const val LED_I = 0x0300


        // Offsets into interrupt status register for different types
        const val ALS_IF_L = 0x0D
        const val ALS_IF_H = 0x0C
        const val PS_IF_CLOSE = 0x09
        const val PS_IF_AWAY = 0x08
    }
}
