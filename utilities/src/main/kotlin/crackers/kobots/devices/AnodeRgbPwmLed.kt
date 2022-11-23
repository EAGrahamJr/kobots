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

import com.diozero.api.DeviceInterface
import com.diozero.devices.RgbPwmLed
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface
import crackers.kobots.utilities.nativeDeviceFactory
import java.awt.Color

/**
 * RGB LED with common anode - this is **inverted**
 * from [RgbPwmLed]. Provides the same interface.
 *
 * Basically, the LED pins are initially _high_ and when the
 * anode is **high**, the light is _off_.
 */
class AnodeRgbPwmLed @JvmOverloads constructor(
    redPin: Int,
    greenPin: Int,
    bluePin: Int,
    deviceFactory: PwmOutputDeviceFactoryInterface = nativeDeviceFactory
) : DeviceInterface {
    private val realLed = RgbPwmLed(deviceFactory, redPin, greenPin, bluePin).apply {
        setValues(1f, 1f, 1f)
    }

    private fun Float.inverted() = 1f - this

    /**
     * Get the value of all LEDs.
     *
     * @return Boolean array (red, green, blue).
     */
    fun getValues(): FloatArray = realLed.values

    /**
     * Set the value of all LEDs.
     *
     * @param red   Red LED value (0..1).
     * @param green Green LED value (0..1).
     * @param blue  Blue LED value (0..1).
     */
    fun setValues(red: Float, green: Float, blue: Float) {
        realLed.setValues(red.inverted(), green.inverted(), blue.inverted())
    }

    /**
     * Sets the [Color] of all the LEDs.
     *
     * @param color The color to set
     */
    fun setColor(color: Color) {
        setValues(color.red / 255f, color.green / 255f, color.blue / 255f)
    }

    /**
     * Turn all LEDs on.
     */
    fun on() {
        realLed.setValues(0f, 0f, 0f)
    }

    /**
     * Turn all LEDs off.
     */
    fun off() {
        realLed.setValues(1f, 1f, 1f)
    }

    /**
     * Toggle the state of all LEDs.
     */
    fun toggle() {
        realLed.toggle()
    }

    override fun close() {
        realLed.close()
    }
}
