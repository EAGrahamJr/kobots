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

import com.diozero.api.DeviceInterface
import com.diozero.api.I2CDevice
import crackers.kobots.devices.expander.AdafruitSeeSaw
import crackers.kobots.devices.expander.NeoPixel
import java.awt.Color

/**
 * Adafruit https://www.adafruit.com/product/4980
 */
class NeoKey(i2CDevice: I2CDevice = DEFAULT_I2C) : DeviceInterface {
    private val seeSaw = AdafruitSeeSaw(i2CDevice)

    val pixels = NeoPixel(seeSaw, 4, NEOPIX_DEVICE).apply {
        brightness = 0.2f
    }

    init {
        INPUT_PINS.forEach {
            seeSaw.pinMode(it, AdafruitSeeSaw.Companion.SignalMode.INPUT_PULLUP)
        }
    }

    /**
     * Get a button value.
     *
     * TODO for some reason, the read on a PULLUP is backwards
     */
    operator fun get(index: Int): Boolean = !seeSaw.digitalRead(INPUT_PINS[index])

    /**
     * Set a pixel to color
     */
    operator fun set(index: Int, color: Color) {
        pixels[index] = color
    }

    /**
     * Set a pixel to color
     */
    operator fun set(index: Int, color: PixelColor) {
        pixels[index] = color
    }

    companion object {
        private val INPUT_PINS = (4..8).toList()
        private const val NEOPIX_DEVICE = 0x03.toByte()

        const val DEFAULT_ADDRESS = 0x30
        val DEFAULT_I2C = I2CDevice(1, DEFAULT_ADDRESS)
    }

    override fun close() {
        pixels.off()
        seeSaw.close()
    }
}
