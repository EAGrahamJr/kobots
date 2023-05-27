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

import crackers.kobots.utilities.GOLDENROD
import crackers.kobots.utilities.PURPLE
import java.awt.Color
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicReference

/**
 * Stuff for a neopixel strip. Using this to register state changes.
 */
object Stripper {
    enum class StripColors {
        DEFAULT, RED, GREEN, BLUE, YELLOW, PURPLE, LARSON;

        fun next(): StripColors = if (this == LARSON) DEFAULT else values()[ordinal + 1]
    }

    private val colorMode = AtomicReference(StripColors.DEFAULT)

    private val neoPixel by lazy {
        crickitHat.neoPixel(30).apply {
            brightness = 0.1f
            autoWrite = false
        }
    }

    private val neopixelMax = 29

    fun start() {
        // TODO change this to a request/event topic, except what to do about "larson" mode?
        checkRun(20) { execute() }
    }

    fun modeChange() = colorMode.get().next().let {
        colorMode.set(it)
        if (it == StripColors.DEFAULT) lastNeoPixelColor = Color.PINK
    }

    fun modeSelect(mode: StripColors) {
        colorMode.set(mode)
    }

    private var lastNeoPixelColor = Color.PINK
    private fun execute() {
        when (colorMode.get()) {
            StripColors.RED -> colorChange(Color.RED)
            StripColors.GREEN -> colorChange(Color.GREEN)
            StripColors.BLUE -> colorChange(Color.BLUE)
            StripColors.YELLOW -> colorChange(GOLDENROD)
            StripColors.PURPLE -> colorChange(PURPLE)
            StripColors.LARSON -> larson()
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
