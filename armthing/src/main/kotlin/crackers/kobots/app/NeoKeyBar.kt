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

package crackers.kobots.app

import crackers.kobots.app.io.NeoKeyHandler

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
