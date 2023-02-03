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
import com.diozero.internal.spi.AbstractDeviceFactory
import com.diozero.internal.spi.AbstractInputDevice
import com.diozero.internal.spi.AnalogInputDeviceFactoryInterface
import com.diozero.internal.spi.AnalogInputDeviceInterface
import com.diozero.sbc.BoardPinInfo
import java.util.*

/**
 * The [ADS7830 ](https://cdn.datasheetspdf.com/pdf-down/A/D/S/ADS7830-etcTI.pdf) is a single-supply, low-power,
 * 8-bit data acquisition device that features a serial I2C interface and an 8-channel multiplexer..
 */
class ADS7830(val i2CDevice: I2CDevice = I2CDevice(1, DEFAULT_ADDRESS)) :
    AbstractDeviceFactory(NAME), AnalogInputDeviceFactoryInterface {

    override fun getName() = NAME

    private val myBoardInfo = BoardPinInfo().apply {
        (0..7).forEach { channel: Int ->
            addAdcPinInfo("Channel", channel, "Channel-$channel", PinInfo.NOT_DEFINED, VREF)
        }
    }

    override fun getBoardPinInfo() = myBoardInfo

    override fun createAnalogInputDevice(key: String, pinInfo: PinInfo) =
        ChannelDevice(pinInfo.deviceNumber, this, key) as AnalogInputDeviceInterface

    companion object {
        const val NAME = "ADS7830"
        const val DEFAULT_ADDRESS = 0x4b
        const val VREF = 255f

        /**
         * The default read command
         */
        private const val CMD = 0x84

        /**
         * Convert the ADC address channel to the I2C register/offset to read from.
         */
        fun channelToRegister(channel: Int): Int {
            val innerMost = ((channel shl 2) or (channel shr 1)) and 0x07
            val shiftLeft = innerMost shl 4
            return CMD or shiftLeft
        }
    }

    /**
     * Read data from the [channel] (0-7).
     */
    fun readFromChannel(channel: Int): Short {
        if (channel !in (0..7)) throw IllegalArgumentException("Channel $channel is out of range (0 to 7)")
        return i2CDevice.readUByte(channelToRegister(channel))
    }

    /**
     * Returns a percentage value (0.0 to 1.0) for the given channel.
     */
    @Deprecated("Channel device is better")
    fun getValue(channel: Int): Float = readFromChannel(channel) / 255.0f

    /**
     * Class to implement an input device.
     */
    private class ChannelDevice(val channel: Int, val parent: ADS7830, deviceKey: String) : AnalogInputDeviceInterface,
        AbstractInputDevice<AnalogInputEvent>(deviceKey, parent) {
        init {
            if (parent.isDeviceOpened(key)) throw DeviceAlreadyOpenedException("Channel $channel in use elsewhere")
            parent.deviceOpened(this)
        }

        override fun generatesEvents() = false

        override fun getAdcNumber() = channel

        override fun getValue() = parent.readFromChannel(channel).toFloat()
    }
}
