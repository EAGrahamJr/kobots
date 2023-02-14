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
import com.diozero.devices.oled.SsdOledCommunicationChannel.I2cCommunicationChannel

/**
 * https://learn.adafruit.com/adafruit-grayscale-1-5-128x128-oled-display?view=all
 *
 * - Arduino
 *   - https://github.com/adafruit/Adafruit_SSD1327
 *   - https://github.com/adafruit/Adafruit-GFX-Library
 *
 * Python - https://github.com/adafruit/Adafruit_CircuitPython_SSD1327
 *
 * Check out https://github.com/OmniXRI/Pi_Pico_OLED_SSD1327_I2C
 */
class SSD1327(delegate: SsdOledCommunicationChannel) :
    GrayOled(delegate, HEIGHT, WIDTH, DisplayType.FOUR_BITS, initSequence(128, 128)) {
    override val dataCommand: Int = DATA_MODE
    override val setColumnCommand: Int = SET_COL_ADDR
    override val setRowCommand: Int = SET_ROW_ADDR

    override fun setDisplayOn(on: Boolean) {
        // enable/disable the VDD regulator, too
        command(
            if (on) intArrayOf(SET_FN_SELECT_A, 0x01, SET_DISP or 0x01)
            else intArrayOf(SET_FN_SELECT_A, 0x00, SET_DISP)
        )
    }

    override fun invertDisplay(invert: Boolean) {
        command(if (invert) SET_DISP_INVERT else SET_DISP_NORMAL)
    }

    override fun close() {
        setDisplayOn(false)
        command(intArrayOf(SET_FN_SELECT_A, 0x00))
        super.close()
    }

    companion object {
        var HEIGHT = 128
        var WIDTH = 128

        const val CMD_MODE = 0x00
        const val DATA_MODE = 0x40

        const val SET_COL_ADDR = 0x15
        const val SET_SCROLL_DEACTIVATE = 0x2E
        const val SET_ROW_ADDR = 0x75
        const val SET_CONTRAST = 0x81
        const val SET_SEG_REMAP = 0xA0
        const val SET_DISP_START_LINE = 0xA1
        const val SET_DISP_OFFSET = 0xA2
        const val SET_DISP_NORMAL = 0xA4
        const val SET_DISP_INVERT = 0xA7

        //        const val DISPLAYALLON = 0xA5
//        const val DISPLAYALLOFF = 0xA6
        const val SET_MUX_RATIO = 0xA8
        const val SET_FN_SELECT_A = 0xAB
        const val SET_DISP = 0xAE     // private in SsdOled

        //        const val DISPLAYON = 0xAF      // private in SsdOled
        const val SET_PHASE_LEN = 0xB1
        const val SET_DISP_CLK_DIV = 0xB3
        const val SET_SECOND_PRECHARGE = 0xB6
        const val SET_GRAYSCALE_TABLE = 0xB8
        const val SET_GRAYSCALE_LINEAR = 0xB9
        const val SET_PRECHARGE = 0xBC
        const val SET_VCOM_DESEL = 0xBE
        const val SET_FN_SELECT_B = 0xD5
        const val SET_COMMAND_LOCK = 0xFD

        fun initSequence(height: Int, width: Int) =
            Math.floorDiv((128 - width), 4).let { elements ->
                intArrayOf(
                    SET_COMMAND_LOCK, 0x12,
                    SET_DISP,
                    SET_DISP_START_LINE, 0x00,
                    SET_DISP_OFFSET, 0x0,
                    SET_SEG_REMAP, 0x51,
                    SET_MUX_RATIO, height - 1,
                    SET_FN_SELECT_A, 0x01,
                    SET_PHASE_LEN, 0x51,
                    SET_DISP_CLK_DIV, 0x01,
                    SET_PRECHARGE, 0x08,
                    SET_VCOM_DESEL, 0x07,
                    SET_SECOND_PRECHARGE, 0x01,
                    SET_FN_SELECT_B, 0x62,
                    SET_GRAYSCALE_LINEAR,
                    SET_CONTRAST, 0x7f,
                    SET_DISP_NORMAL,
                    SET_ROW_ADDR, 0x00, height - 1,
                    SET_COL_ADDR, elements, 63 - elements,
                    SET_SCROLL_DEACTIVATE,
                    SET_DISP or 0x01
                )
            }

        // Adafruit default address and device
        const val ADAFRUIT_I2C_ADDRESS = 0x3D
        val ADAFRUIT_STEMMA by lazy { I2cCommunicationChannel(I2CDevice(1, ADAFRUIT_I2C_ADDRESS)) }
    }
}
