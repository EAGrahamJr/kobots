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
import java.util.*

/**
 * Delegate to I2C Devices for the ADS7830 ([datasheet](https://cdn.datasheetspdf.com/pdf-down/A/D/S/ADS7830-etcTI.pdf)).
 *
 * Uses the Raspberry PI default controller/bus and a default I2C address. Currently does **NOT**
 * support high-speed modes, as it may not be desirable to "clog" the I2C bus.
 */
object ADS7830 {
    const val DEFAULT_ADDRESS = 0x4b

    val defaultDevice by lazy {
        I2CDevice(1, DEFAULT_ADDRESS).also {
            if (!it.probe()) throw IllegalStateException("Default I2C address is not available")
        }
    }

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
     * Read data from the [channel] (0-7).
     */
    fun readFromChannel(channel: Int, i2CDevice: I2CDevice = defaultDevice): Short {
        if (channel !in (0..7)) throw IllegalArgumentException("Channel $channel is out of range (0 to 7)")
        return i2CDevice.readUByte(channelToRegister(channel))
    }

    /**
     * Read data from the [channel] (0-7), without throwing an exception
     */
    fun readFromChannelSafely(channel: Int, i2CDevice: I2CDevice = defaultDevice): Optional<Short> {
        if (channel !in (0..7)) throw IllegalArgumentException("Channel $channel is out of range (0 to 7)")
        return try {
            Optional.of(i2CDevice.readUByte(channelToRegister(channel)))
        } catch (_: Throwable) {
            Optional.empty()
        }
    }

    /**
     * Returns a percentage value (0.0 to 1.0) for the given channel.
     */
    fun getValue(channel: Int, i2CDevice: I2CDevice = defaultDevice): Float = readFromChannel(channel, i2CDevice) / 255.0f

    /**
     * Returns a percentage value (0.0 to 1.0) for the given channel, without throwing an exception.
     */
    fun getValueSafely(channel: Int, i2CDevice: I2CDevice = defaultDevice): Optional<Float> =
        readFromChannelSafely(channel, i2CDevice).let { opt ->
            if (opt.isPresent) Optional.of(opt.get() / 255.0f)
            else Optional.empty()
        }

    /**
     * A convenience function for a generic thermistor attached to a channel.
     */
    fun getTemperature(channel: Int, i2CDevice: I2CDevice = defaultDevice, referenceVoltage: Float = 3.3f): Float =
        getTemperature(getValue(channel, i2CDevice).toDouble(), referenceVoltage)
}
