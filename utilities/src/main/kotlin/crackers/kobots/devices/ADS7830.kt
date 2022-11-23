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

package crackers.kobots.devices

import com.diozero.api.I2CDevice
import java.lang.Math.log

/**
 * Delegate to I2C Devices for the ADS7830 ([datasheet](https://cdn.datasheetspdf.com/pdf-down/A/D/S/ADS7830-etcTI.pdf)).
 *
 * Uses the Raspberry PI default controller/bus and a default I2C address. Currently does **NOT**
 * support high-speed modes, as it may not be desirable to "clog" the I2C bus.
 */
object ADS7830 {
    const val DEFAULT_ADDRESS = 0x4b

    val defaultDevice by lazy { I2CDevice(1, DEFAULT_ADDRESS) }

    /**
     * The default read command
     */
    private val CMD = 0x84

    /**
     * Convert the ADC address channel to the I2C register/offset to read from.
     */
    fun channelToRegister(channel: Int): Int {
        val innerMost = ((channel shl 2) or (channel shr 1)) and 0x07
        val shiftLeft = innerMost shl 4
        return CMD or shiftLeft
    }

    /**
     * Read data from the []channel].
     */
    fun readFromChannel(channel: Int, i2CDevice: I2CDevice = defaultDevice): Short {
        if (channel !in (0..7)) throw IllegalArgumentException("Channel $channel is out of range (0 to 7)")
        return i2CDevice.readUByte(channelToRegister(channel))
    }

    /**
     * Returns a percentage value (0.0 to 1.0) for the given channel.
     */
    fun getValue(channel: Int, i2CDevice: I2CDevice = defaultDevice): Float = readFromChannel(channel, i2CDevice) / 255.0f

    const val KELVIN = 273.15

    /**
     * A convenience function for a generic thermistor attached to a channel.
     *
     * The formula given is `T2 = 1/(1/T1 + ln(Rt/R)/B)`
     */
    fun getTemperature(channel: Int, i2CDevice: I2CDevice = defaultDevice, referenceVoltage: Float = 3.3f): Float {
        val voltage = referenceVoltage * getValue(channel, i2CDevice).toDouble()
        // NOTE: this would normally be multiplied by the reference resistence, but that is divided back out in the formula
        val voltageRatio = voltage / (referenceVoltage - voltage)
        // 25 is the "reference" temperature
        // 3950 is the "thermal index"
        val k = 1 / (1 / (KELVIN + 25) + log(voltageRatio) / 3950.0)
        println("V $voltage R $voltageRatio K $k")
        return (k - KELVIN).toFloat()
    }

    fun Float.asDegreesF(): Float = (this * 9 / 5) + 32.0f
}
