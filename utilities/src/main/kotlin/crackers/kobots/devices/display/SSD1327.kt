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

package crackers.kobots.devices.display

import com.diozero.api.I2CDevice
import com.diozero.devices.oled.SsdOledCommunicationChannel

/**
 * https://learn.adafruit.com/adafruit-grayscale-1-5-128x128-oled-display?view=all
 *
 * - Arduino
 *   - https://github.com/adafruit/Adafruit_SSD1327
 *   - https://github.com/adafruit/Adafruit-GFX-Library
 *
 * Python - https://github.com/adafruit/Adafruit_CircuitPython_SSD1327
 */
class SSD1327(delegate: SsdOledCommunicationChannel) :
    GrayOled(delegate, HEIGHT, WIDTH, DisplayType.FOUR_BITS, INIT_SEQUENCE) {
    override fun dataCommand(): Int = DATA_MODE

    override fun setDisplayOn(on: Boolean) {
        oledSendCommand(if (on) DISPLAYON else DISPLAYOFF)
    }

    override fun invertDisplay(invert: Boolean) {
        oledSendCommand(INVERTDISPLAY)
    }

    override fun close() {
        setDisplayOn(false)
        oledSendCommandList(intArrayOf(REGULATOR, 0x00))
        super.close()
    }

    companion object {
        var HEIGHT = 128
        var WIDTH = 128

        const val CMD_MODE = 0x00
        const val DATA_MODE = 0x40

        const val SSD1305_SETBRIGHTNESS = 0x82

        const val SETCOLUMN = 0x15

        const val SETROW = 0x75

        const val SETCONTRAST = 0x81

        const val SSD1305_SETLUT = 0x91

        const val SEGREMAP = 0xA0
        const val SETSTARTLINE = 0xA1
        const val SETDISPLAYOFFSET = 0xA2
        const val NORMALDISPLAY = 0xA4
        const val DISPLAYALLON = 0xA5
        const val DISPLAYALLOFF = 0xA6
        const val INVERTDISPLAY = 0xA7
        const val SETMULTIPLEX = 0xA8
        const val REGULATOR = 0xAB
        const val DISPLAYOFF = 0xAE     // private in SsdOled
        const val DISPLAYON = 0xAF      // private in SsdOled

        const val PHASELEN = 0xB1
        const val DCLK = 0xB3
        const val PRECHARGE2 = 0xB6
        const val GRAYTABLE = 0xB8
        const val PRECHARGE = 0xBC
        const val SETVCOM = 0xBE

        const val FUNCSELB = 0xD5

        const val CMDLOCK = 0xFD

        val INIT_SEQUENCE = intArrayOf(
            DISPLAYOFF, // DISPLAY_OFF
            0x00,
            // TODO expose this?
            SETCONTRAST, // set contrast control
            0x01,
            0x80,
            SEGREMAP, // remap memory, odd even columns, com flip and column swap
            0x01,
            0x53,
            SETSTARTLINE, // Display start line is 0
            0x01,
            0x00,
            SETDISPLAYOFFSET, // Display offset is 0
            0x01,
            0x00,
            NORMALDISPLAY, // Normal display
            0x00,
            SETMULTIPLEX, // Mux ratio is 1/64
            0x01,
            0x3f,
            PHASELEN, // Set phase length
            0x01,
            0x11,
            GRAYTABLE, // gray-scale table
            0x0f,
            0x00,
            0x01,
            0x02,
            0x03,
            0x04,
            0x05,
            0x06,
            0x07,
            0x08,
            0x10,
            0x18,
            0x20,
            0x2f,
            0x38,
            0x3f,
            DCLK,   // Set dclk to 100hz
            0x01,
            0x00,
            REGULATOR, // enable internal regulator - note: this will get turned OFF on device close
            0x01,
            0x01,
            PRECHARGE2, // Set second pre-charge period
            0x01,
            0x04,
            SETVCOM, // Set vcom voltage
            0x01,
            0x0f,
            PRECHARGE, // Set pre-charge voltage
            0x01,
            0x08,
            FUNCSELB, // function selection B
            0x01,
            0x62,
            CMDLOCK, // command unlock
            0x01,
            0x12,
            DISPLAYON, // DISPLAY_ON
            0x00
        )

        // Adafruit default address and device
        const val ADAFRUIT_I2C_ADDRESS = 0x3D
        val ADAFRUIT_STEMMA by lazy { I2cCommunicationChannel(I2CDevice(1, ADAFRUIT_I2C_ADDRESS)) }
    }
}
