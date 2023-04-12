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

import com.diozero.sbc.LocalSystemInfo
import crackers.kobots.devices.display.GrayOled
import crackers.kobots.devices.display.SSD1327
import crackers.kobots.utilities.center
import crackers.kobots.utilities.loadImage
import java.awt.*
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Displays some system info for a while, before sleeping, and then going dark.
 */
object Screen {
    private val screenGraphics: Graphics2D
    private val image: BufferedImage
    private val timeFont = Font("Helvetica", Font.BOLD, 18)
    private val timeFontMetrics: FontMetrics

    private val screen: GrayOled by lazy { SSD1327(SSD1327.ADAFRUIT_STEMMA) }

    private val okStatusImage = loadImage("/smiley.png")
    private val alertStatusImage = loadImage("/not-smiley.jpg")
    private val sleepingImage = loadImage("/snooze.jpg")
    private var sleeping = false
    const val MAX_TIME = 60
    const val MAX_SLEEP = 30

    init {
        image = BufferedImage(128, 128, BufferedImage.TYPE_BYTE_GRAY).also {
            screenGraphics = it.graphics as Graphics2D
        }
        timeFontMetrics = screenGraphics.getFontMetrics(timeFont)

        screen.clear()
        screen.displayOn = true
        with(screenGraphics) {
            // draw a thermometer, kind of
            color = Color.GRAY
            drawRect(0, 0, 4, 127)

            color = Color.WHITE
            font = Font(Font.SANS_SERIF, Font.PLAIN, 10)
            val fh = getFontMetrics(font).ascent
            drawString("80", 6, fh + 1)
            drawString("40", 6, 126)
            drawString("\u2103", 6, 63 + fh / 2)
        }
    }

    fun isOn() = screen.displayOn
    fun close() = screen.close()

    private var lastDisplayed = Instant.now().minusSeconds(MAX_SLEEP.toLong())

    fun startTimeAndTempThing() {
        lastDisplayed = Instant.now()
        screen.displayOn = true
        sleeping = false
        displayCurrentStatus()
    }

    /**
     * Display the application statuses. Checks to see how long the display has been on and will shut it off after a
     * period of time
     */
    fun displayCurrentStatus() = with(screenGraphics) {
        showTemperature()
        showTime()
        showProximity()

        // update the screen
        if (applyImage()) {
            showIt()
        }
    }

    private fun Graphics2D.showTemperature() {
        color = Color.BLACK
        fillRect(1, 1, 3, 126)
        val temperature = getTemperature()
        color = when {
            temperature < 55 -> Color.GREEN
            temperature < 70 -> Color.YELLOW
            else -> Color.RED
        }

        // offset by 40 as that's our lower bound
        val diff = (max(temperature - 40, 0f) * 125 / 40).roundToInt()
        fillRect(1, 126 - diff, 3, 126)
    }

    // use these to offset from the thermometer
    private const val LEFTMOST = 20
    private const val FILLX = 110

    private fun Graphics2D.showTime() {
        color = Color.BLACK
        fillRect(LEFTMOST, 0, FILLX, timeFontMetrics.height + 1)
        font = timeFont
        val time = LocalTime.now().let { String.format("%2d:%02d:%02d", it.hour, it.minute, it.second) }
        val tx = LEFTMOST + timeFontMetrics.center(time, 107)
        color = Color.CYAN
        drawString(time, tx, timeFontMetrics.ascent + 1)
    }

    private fun Graphics2D.showProximity() {
        color = Color.BLACK
        fillRect(LEFTMOST, timeFontMetrics.height + 2, FILLX, timeFontMetrics.height + 1)
        font = timeFont
        val str = "Prox: ${StatusFlags.proximityReading.get()}"
        val x = LEFTMOST + timeFontMetrics.center(str, 107)
        color = Color.YELLOW
        drawString(str, x, 2 * (timeFontMetrics.height + 1))
    }

    private fun applyImage(): Boolean {
        val now = Instant.now()
        // check the timer - turn the screen off if on too long
        if (screen.displayOn) {
            val howLong = Duration.between(lastDisplayed, now).toSeconds()
            // turn it off
            if (howLong >= MAX_TIME) {
                screen.displayOn = false
                sleeping = false
            } else if (!sleeping && howLong >= MAX_SLEEP) {
                // screen remains on for now, but we don't actually want to update it again
                sleeping = true
                displayImage(sleepingImage)
                showIt()
            }
        }
        return !sleeping && screen.displayOn
    }

    /**
     * Display the current image buffer.
     */
    private fun showIt() {
        screen.display(image)
        screen.show()
    }

    private fun getTemperature(): Float = LocalSystemInfo.getInstance().cpuTemperature

    private fun displayImage(image: Image) {
        screenGraphics.drawImage(image, 22, 22, 106, 106, null)
    }
}
