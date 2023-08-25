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

package crackers.kobots.app.io

import crackers.kobots.devices.io.NeoKey
import crackers.kobots.devices.lighting.WS2811
import org.tinylog.Logger
import java.awt.Color

/**
 * Handles getting button presses from a NeoKey1x4, along with setting pretty colors, etc. Performs a single
 * "debounce" by only reporting a button press if it's different from the last read. This works well in a fairly
 * "tight" loop, but if you're doing other things in between, you may want to do your own debouncing.
 * @param keyboard the NeoKey1x4 to use
 * @param activationColor the color to use when a button is pressed
 * @param initialColors the initial set of colors to use when a button is _not_ pressed (default blue, green, cyan, red)
 * @param initialBrightness the initial brightness of the keyboard (default 0.05)
 *
 * TODO allow for multiplexing multiple NeoKey1x4s
 */
class NeoKeyHandler(
    private val keyboard: NeoKey = NeoKey(),
    private val activationColor: Color = Color.YELLOW,
    initialColors: List<Color> = listOf(Color.BLUE, Color.GREEN, Color.CYAN, Color.RED),
    initialBrightness: Float = 0.05f
) : AutoCloseable {

    private var _colors: List<Color> = initialColors.toList()

    init {
        keyboard.brightness = initialBrightness
        updateButtonColors()
    }

    private val NO_BUTTONS = listOf(false, false, false, false)
    private var lastButtonValues = NO_BUTTONS

    override fun close() {
        keyboard.close()
    }

    /**
     * The current brightness of the keyboard (get/set).
     */
    var brightness: Float
        get() = keyboard.brightness
        set(value) {
            keyboard.brightness = value
            updateButtonColors(true)
        }

    private val SINGLE_KEYBOARD = 4

    /**
     * The number of buttons handled. This is to allow for expansion capabilities.
     */
    val numberOfButtons: Int
        get() = SINGLE_KEYBOARD

    var buttonColors: List<Color>
        get() = _colors
        set(c) {
            _colors = c.toList()
            updateButtonColors()
        }

    /**
     * Update the colors of the buttons when not pressed, only if necessary.
     */
    private fun updateButtonColors(force: Boolean = false) {
        val newColors = buttonColors.map { WS2811.PixelColor(it) }
        if (newColors != keyboard.colors() || force) keyboard[0, 3] = newColors
    }

    /**
     * Read the current button presses, and set the colors of the buttons as needed. Any "active" buttons will be
     * set to the [activationColor].
     */
    fun read(): List<Boolean> = try {
        keyboard.read().let { read ->
            // nothing changed, make sure buttons are the "right" color
            if (read == lastButtonValues) {
                updateButtonColors()
                NO_BUTTONS
            } else {
                lastButtonValues = read
                read.also {
                    it.forEachIndexed { index, b ->
                        keyboard.pixels[index] = if (b) activationColor else buttonColors[index]
                    }
                }
            }
        }
    } catch (e: Exception) {
        Logger.error(e, "Error reading keyboard")
        NO_BUTTONS
    }
}
