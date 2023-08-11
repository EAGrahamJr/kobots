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

package crackers.kobots.execution

import crackers.kobots.devices.io.NeoKey
import crackers.kobots.devices.lighting.WS2811
import org.tinylog.Logger
import java.awt.Color

/**
 * Simple wrapper around a NeoKey1x4, with some convenience methods.
 */
object NeoKeyBar {
    private val keyboard = NeoKeyHandler()
    var brightness: Float
        get() = keyboard.brightness
        set(value) {
            keyboard.brightness = value
        }
    private lateinit var _currentButtons: List<Boolean>
    val currentButtons: List<Boolean>
        get() = _currentButtons

    /**
     * Check for button presses, and set the colors of the buttons.
     * @return true if the "exit" button (3) was pressed
     */
    fun buttonCheck(): Boolean {
        _currentButtons = keyboard.read()
        return currentButtons.isEmpty() || !currentButtons[3]
    }

    fun close() {
        keyboard.close()
    }
}

/**
 * Handles getting button presses from a NeoKey1x4, along with setting pretty colors, etc. Performs a single
 * "debounce" by only reporting a button press if it's different from the last read.
 */
class NeoKeyHandler(
    private val keyboard: NeoKey = NeoKey(),
    private val activationColor: Color = Color.YELLOW,
    private var buttonColors: List<Color> = listOf(Color.BLUE, Color.GREEN, Color.CYAN, Color.RED),
    initialBrightness: Float = 0.05f
) : AutoCloseable {

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
        }

    /**
     * Update the colors of the buttons when not pressed.
     */
    fun setButtonColors(colors: List<Color>) {
        buttonColors = colors
        updateButtonColors()
    }

    /**
     * Update the colors of the buttons when not pressed, only if necessary.
     */
    private fun updateButtonColors() {
        val newColors = buttonColors.map { WS2811.PixelColor(it) }
        if (newColors != keyboard.colors()) keyboard[0, 3] = newColors
    }

    /**
     * Read the current button presses, and set the colors of the buttons as needed. Any "active" buttons will be
     * set to the [activationColor].
     */
    fun read() = try {
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
