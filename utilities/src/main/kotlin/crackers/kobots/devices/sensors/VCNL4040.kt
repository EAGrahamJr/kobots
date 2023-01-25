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
 * * [Datasheet](https://www.vishay.com/docs/84274/vcnl4040.pdf)
 * * [Adafruit guide](https://learn.adafruit.com/adafruit-vcnl4040-proximity-sensor?view=all)
 */
class VCNL4040 @JvmOverloads constructor(controller: Int = 1, address: Int = ADDRESS) : LuminositySensorInterface {
    // Ambient light sensor integration times - ordinal matches data
    enum class AmbientLightIntegrationTime(m: Int) {
        ALS_80MS(80), ALS_160MS(160), ALS_320MS(320), ALS_640MS(640);

        val millis: Int = m
    }

    enum class AmbientLightPersistence {
        ONE, TWO, FOUR, EIGHT
    }

    // Proximity sensor integration times - ordinal matches data
    enum class ProximityIntegrationTime {
        PS_1T, PS_1_5T, PS_2T, PS_2_5T, PS_3T, PS_3_5T, PS_4T, PS_8T
    }

    enum class ProximityMultipulse {
        ONE, TWO, FOUR, EIGHT
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
    enum class ProximityInterruptType {
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
    }

    fun dumpRegisters() {
        (0..0x0c).forEach {
            println("Register ${Hex.encode(it)} - ${Hex.encode(delegate.readWordData(it))}")
        }
    }

    override fun close() {
        delegate.close()
    }

    // scale the lux depending on the value of the integration time
    override fun getLuminosity(): Float = (ambientLight * (0.1 / (1 shl ambientLightIntegrationTime.ordinal))).toFloat()

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

    // read and clear interrupt ---------------------------------------------------------------------------------------
    val interruptStatus: Int
        get() {
            return delegate.shortSubRegister(INT_FLAG, INT_MASK).read().toInt()
        }

    // bit masks for the interrupt status
    fun isEnteringProtectionMode(interruptStatus: Int) = interruptStatus and 0x40
    fun isAmbientLightLow(interruptStatus: Int) = interruptStatus and 0x20
    fun isAmbientLightHigh(interruptStatus: Int) = interruptStatus and 0x10
    fun isProximityClose(interruptStatus: Int) = interruptStatus and 0x02
    fun isProximityFar(interruptStatus: Int) = interruptStatus and 0x01

    // Thresholds -----------------------------------------------------------------------------------------------------
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

    //-----------------------------------------------------------------------------------------------------------------
    // Configuration
    private fun writeBit(register: Int, mask: Int, enable: Boolean) {
        // read the register and flip the indicated bit
        val output = (if (enable) 1 else 0).toShort()
        delegate.shortSubRegister(register, mask).write(output)
    }

    private fun writeValue(e: Enum<*>, register: Int, mask: Int) =
        delegate.shortSubRegister(register, mask).write(e.ordinal.toShort())

    // ALS Config -----------------------------------------------------------------------------------------------------

    private val ambientLightTimeRegister by lazy { delegate.shortSubRegister(ALS_CONFIG, ALS_IT) }

    // cache this - it's used in calculations above so doesn't need to be fetched every time (prevent 2 reads)
    private val ambientIntegrationValue: AtomicInteger by lazy {
        AtomicInteger(ambientLightTimeRegister.read().toInt())
    }

    var ambientLightIntegrationTime: AmbientLightIntegrationTime
        get() {
            return AmbientLightIntegrationTime.values()[ambientIntegrationValue.get()]
        }
        set(integrationTime) {
            val newOrdinal = integrationTime.ordinal
            ambientLightTimeRegister.write(newOrdinal.toShort())
            ambientIntegrationValue.set(newOrdinal)
        }

    var ambientLightInterruptPersistence: AmbientLightPersistence
        get() {
            val ordinal = delegate.shortSubRegister(ALS_CONFIG, ALS_PERS).read().toInt()
            return AmbientLightPersistence.values()[ordinal]
        }
        set(ambientLightPersistence) = writeValue(ambientLightPersistence, ALS_CONFIG, ALS_PERS)

    var ambientLightInterruptsEnabled: Boolean
        get() = delegate.shortSubRegister(ALS_CONFIG, ALS_INT_EN).read() == 0.toShort()
        set(enable) = writeBit(ALS_CONFIG, ALS_INT_EN, enable)

    /**
     * The bit for this flag is actually _inverted_ as it's technically an "is shutdown" flag
     */
    var ambientLightEnabled: Boolean
        get() = !(delegate.shortSubRegister(ALS_CONFIG, ALS_SD).read() == 0.toShort())
        set(enable) = writeBit(ALS_CONFIG, ALS_SD, !enable)

    // PS Config 1 ----------------------------------------------------------------------------------------------------
    var proximityLEDDutyCycle: LEDDutyCycle
        get() {
            val current = delegate.shortSubRegister(PS_CONFIG1, PS_DUTY).read().toInt()
            return LEDDutyCycle.values()[current]
        }
        set(dutyCycle) = writeValue(dutyCycle, PS_CONFIG1, PS_DUTY)

    var proximityInterruptPersistence: Int
        get() = delegate.shortSubRegister(PS_CONFIG1, PS_PERS).read().toInt() + 1
        set(proximityInterruptPersistence) =
            delegate.shortSubRegister(PS_CONFIG1, PS_PERS).write((proximityInterruptPersistence - 1).toShort())

    var proximityIntegrationTime: ProximityIntegrationTime
        get() {
            val ordinal = delegate.shortSubRegister(PS_CONFIG1, PS_IT).read().toInt()
            return ProximityIntegrationTime.values()[ordinal]
        }
        set(integrationTime) = writeValue(integrationTime, PS_CONFIG1, PS_IT)

    /**
     * The bit for this flag is actually _inverted_ as it's technically an "is shutdown" flag
     */
    var proximityEnabled: Boolean
        get() = !(delegate.shortSubRegister(PS_CONFIG1, PS_SD).read() == 0.toShort())
        set(enable) = writeBit(PS_CONFIG1, PS_SD, !enable)

    // PS Config 2 ----------------------------------------------------------------------------------------------------
    var proximityHighResolution: ProximityResolution
        get() {
            val current = delegate.shortSubRegister(PS_CONFIG2, PS_HD).read().toInt()
            return ProximityResolution.values()[current]
        }
        set(highResolution) = writeValue(highResolution, PS_CONFIG2, PS_HD)

    var proximityInterrupts: ProximityInterruptType
        get() {
            val ordinal = delegate.shortSubRegister(PS_CONFIG2, PS_INT).read().toInt()
            return ProximityInterruptType.values()[ordinal]
        }
        set(interruptCondition) = writeValue(interruptCondition, PS_CONFIG2, PS_INT)


    // PS Config 3 ----------------------------------------------------------------------------------------------------
    var proximityMultipulse: ProximityMultipulse
        get() {
            val current = delegate.shortSubRegister(PS_CONFIG3, PS_MPS).read().toInt()
            return ProximityMultipulse.values()[current]
        }
        set(highResolution) = writeValue(highResolution, PS_CONFIG3, PS_MPS)

    var proximitySmartPersistenceEnabled: Boolean
        get() = delegate.shortSubRegister(PS_CONFIG3, PS_SMART_PERS).read() == 0.toShort()
        set(enable) = writeBit(PS_CONFIG3, PS_SMART_PERS, enable)

    var proximityActiveForceEnabled: Boolean
        get() = delegate.shortSubRegister(PS_CONFIG3, PS_AF).read() == 0.toShort()
        set(enable) = writeBit(PS_CONFIG3, PS_AF, enable)

    /**
     * Outputs one data cycle - see p. 10
     */
    fun activeForceModeTrigger() {
        writeBit(PS_CONFIG3, PS_TRIG, true)
    }

    var sunlightCancelEnabled: Boolean
        get() = delegate.shortSubRegister(PS_CONFIG3, PS_SC_EN).read() == 0.toShort()
        set(enable) = writeBit(PS_CONFIG3, PS_SC_EN, enable)

    // PS MS ----------------------------------------------------------------------------------------------------------
    /**
     * The bit for this flag is actually _inverted_
     */
    var whiteLightEnabled: Boolean
        get() = !(delegate.shortSubRegister(PS_MS_H, WHITE_EN).read() == 0.toShort())
        set(enable) = writeBit(PS_MS_H, WHITE_EN, !enable)

    var proximityOperation: ProximityOperation
        get() {
            val ordinal = delegate.shortSubRegister(PS_MS_H, PS_MS).read().toInt()
            return ProximityOperation.values()[ordinal]
        }
        set(proximityOperation) = writeValue(proximityOperation, PS_MS_H, PS_MS)

    var proximityLEDCurrent: LEDCurrent
        get() {
            val ordinal = delegate.shortSubRegister(PS_MS_H, LED_I).read().toInt()
            return LEDCurrent.values()[ordinal]
        }
        set(ledCurrent) = writeValue(ledCurrent, PS_MS_H, LED_I)

    // convenience ----------------------------------------------------------------------------------------------------

    companion object {
        const val ADDRESS = 0x60
        const val DEVICE_ID = 0x0186

        // the device uses 16-bit registers
        private const val ALS_CONFIG = 0x00     // Ambient light sensor configuration register
        private const val ALS_THDH = 0x01       // Ambient light high threshold register
        private const val ALS_THDL = 0x02       // Ambient light low threshold register
        private const val PS_CONFIG1 = 0x03     // Proximity sensor configuration 1 register (low byte)
        private const val PS_CONFIG2 = 0x03     // Proximity sensor configuration 2 register (high byte)
        private const val PS_CONFIG3 = 0x04     // Proximity sensor configuration 3 register (low byte)
        private const val PS_MS_H = 0x04        // Proximity sensor misc config register (high byte)
        private const val PS_THDL = 0x06        // Proximity sensor low threshold register
        private const val PS_THDH = 0x07        // Proximity sensor high threshold register
        private const val PS_DATA = 0x08        // Proximity sensor data register
        private const val ALS_DATA = 0x09       // Ambient light sensor data register
        private const val WHITE_DATA = 0x0A     // White light sensor data register
        private const val INT_FLAG = 0x0B       // Interrupt status register
        private const val ID_REGISTER = 0x0C    // Device ID

        // ALS_CONF bits - this is the lower byte of the word: the high-byte is reserved
        private const val ALS_IT = 0x00C0         // integration time (2-bits - AmbientIntegration)

        // RESERVED 0b00110000
        private const val ALS_PERS = 0x000C       // interrupt persistence (2-bits)
        private const val ALS_INT_EN = 0x0002     // interrupt enable/disable (1 == enable)
        private const val ALS_SD = 0x0001         // als power (0 == enable)

        // PS_CONF1 bits - this is the lower byte of the PS_CONFIG word
        private const val PS_DUTY = 0x00C0        // duty ration setting (2-bits - LEDDutyCycle)
        private const val PS_PERS = 0x0030        // interrupt persistence (2-bits)
        private const val PS_IT = 0x000E          // integration time (2-bits - ProximityIntegration)
        private const val PS_SD = 0x0001          // ps power (0 == enable)

        // PS_CONF2 bits - this is the upper byte of the PS_CONFIG word
        // RESERVED 0b11000000
        // RESERVED 0b00110000
        private const val PS_HD = 0x0800          // ps resolution (ProximityResolution)

        // RESERVED 0b00000100
        private const val PS_INT = 0x0300         // ps interrupt type (2-bits - ProximityType)

        // PS_CONF3 bits - this is the lower byte of the PS_CONF3/PS_MS_H register
        // RESERVED 0b1000000
        private const val PS_MPS = 0x0060         // proximity multi-pulse numbers
        private const val PS_SMART_PERS = 0x0010  // smart persistence
        private const val PS_AF = 0x0008          // active force mode (normally 0)
        private const val PS_TRIG = 0x0004        // trigger output cycle

        // RESERVED 0x02
        private const val PS_SC_EN = 0x0001       // sunlight cancellation function

        // PS_MS_H bits - this is the upper byte of the PS_CONF3/PS_MS_H register
        private const val WHITE_EN = 0x8000       // white channel enabled (0 == enabled)
        private const val PS_MS = 0x4000          // proximity function (ProximityOperation)

        // RESERVED 0b00111000
        private const val LED_I = 0x0300

        // Interrupt status register - low byte reserved
        private const val INT_MASK = 0xFF00
    }
}
