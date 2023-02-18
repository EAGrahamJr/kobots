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

package crackers.kobots.devices.lighting

import com.diozero.api.I2CDevice
import java.awt.Color

private val DEFAULT_SHIM_ADDRESS = 0x75
private val DEFAULT_SHIM_DEVICE = I2CDevice(1, DEFAULT_SHIM_ADDRESS)

class PimoroniLEDShim(shimDevice: I2CDevice = DEFAULT_SHIM_DEVICE) : IS31FL3731(shimDevice) {
    override val height = 3
    override val width = 28

    private fun setPixel(x: Int, y: Int, what: Int) {
        super.pixel(x, y, what, false, getFrame())
    }

    fun pixelRGB(x: Int, r: Int, g: Int, b: Int) {
        setPixel(x, 0, r)
        setPixel(x, 1, g)
        setPixel(x, 2, b)
    }

    fun pixelColor(x: Int, color: Color) {
        setPixel(x, 0, color.red)
        setPixel(x, 1, color.green)
        setPixel(x, 2, color.blue)
    }

    override fun pixelAddress(x: Int, y: Int): Int {
        return when (y) {
            0 -> {
                when {
                    x < 7 -> 118 - x
                    x < 15 -> 141 - x
                    x < 21 -> 106 + x
                    x == 21 -> 15
                    else -> x - 14
                }
            }

            1 -> {
                when {
                    x < 2 -> 69 - x
                    x < 7 -> 86 - x
                    x < 12 -> 28 - x
                    x < 14 -> 45 - x
                    x == 14 -> 47
                    x == 15 -> 41
                    x < 21 -> x + 9
                    x == 21 -> 95
                    x < 26 -> x + 67
                    else -> x + 50
                }
            }

            else -> when {
                x == 0 -> 85
                x < 7 -> 102 - x
                x < 11 -> 44 - x
                x < 14 -> 61 - x
                x == 14 -> 63
                x < 17 -> 42 + x
                x < 21 -> x + 25
                x == 21 -> 111
                x < 27 -> x + 83
                else -> 93
            }
        }
    }
}
