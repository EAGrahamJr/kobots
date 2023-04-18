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

import crackers.kobots.app.StatusFlags.colorMode
import crackers.kobots.utilities.PURPLE
import java.awt.Color
import java.time.LocalTime

/**
 * Stuff for a neopixel strip.
 */
object Stripper {
    enum class NeoPixelControls {
        DEFAULT, RED, GREEN, BLUE, LARSON;

        fun next(): NeoPixelControls = if (this == LARSON) DEFAULT else values()[ordinal + 1]
    }

    private val neoPixel by lazy {
        crickitHat.neoPixel(30).apply {
            brightness = 0.1f
            autoWrite = false
        }
    }

    private val neopixelMax = 29

    fun modeChange() = colorMode.get().next().let {
        colorMode.set(it)
        if (it == NeoPixelControls.DEFAULT) lastNeoPixelColor = Color.PINK
    }

    private var lastNeoPixelColor = Color.PINK
    fun execute() {
        when (colorMode.get()) {
            NeoPixelControls.RED -> colorChange(Color.RED)
            NeoPixelControls.GREEN -> colorChange(Color.GREEN)
            NeoPixelControls.BLUE -> colorChange(Color.BLUE)
            NeoPixelControls.LARSON -> larson()
            else -> timeCheck()
        }
    }

    private fun colorChange(color: Color) {
        if (color != lastNeoPixelColor) {
            neoPixel.apply {
                fill(color)
                show()
            }
            lastNeoPixelColor = color
        }
    }

    /**
     * Manage the color of the NeoPixel strip based on time and the "strip mode".
     */
    private val timeCheck = {
        val time = LocalTime.now()
        val stripColor = when {
            time.hour >= 23 && time.minute >= 30 -> Color.BLACK
            time.hour >= 21 -> Color.RED
            time.hour >= 8 -> PURPLE
            else -> Color.BLACK
        }
        colorChange(stripColor)
    }

    private val otherColor = Color(90, 0, 0)
    private var center = -1
    private var direction = 1
    private var previous = -1

    fun larson() {
        // wipe out previous
        val paintItBlack = if (previous != -1) {
            // heading right, make left-most black
            if (direction > 0) {
                center - 1
            } else {
                center + 1
            }
        } else {
            -1
        }

        if (direction > 0) {
            center++
            if (center > neopixelMax) {
                direction = -1
                center = 28
            }
        } else {
            center--
            if (center < 0) {
                center = 1
                direction = 1
            }
        }

        with(neoPixel) {
            if (paintItBlack in (0..neopixelMax)) this[paintItBlack] = Color.BLACK
            this[center] = Color.RED
            if (center - 1 >= 0) this[center - 1] = otherColor
            if (center + 1 <= neopixelMax) this[center + 1] = otherColor
            show()
        }
        previous = center
    }
}
