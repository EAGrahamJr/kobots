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

import crackers.kobots.devices.display.GrayOled
import crackers.kobots.devices.display.SSD1327
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import javax.imageio.ImageIO
import kotlin.math.max

object Screen {
    private val screenGraphics: Graphics2D
    private val image: BufferedImage
    private val timeFont = Font(Font.SERIF, Font.PLAIN, 12)
    private val timeFontMetrics: FontMetrics
    private var distanceFont = Font(Font.MONOSPACED, Font.BOLD, 24)
    private var distanceFontMetrics: FontMetrics

    private val screen: GrayOled by lazy { SSD1327(SSD1327.ADAFRUIT_STEMMA) }

    private val okStatusImage = loadImage("/smiley.png")
    private val alertStatusImage = loadImage("/not-smiley.jpg")
    private val sleepingImage = loadImage("/snooze.jpg")
    private var sleeping = true

    init {
        image = BufferedImage(128, 128, BufferedImage.TYPE_BYTE_GRAY).also {
            screenGraphics = it.graphics as Graphics2D
        }
        timeFontMetrics = screenGraphics.getFontMetrics(timeFont)
        distanceFontMetrics = screenGraphics.getFontMetrics(distanceFont)

        // TODO maybe not?
        screen.display(sleepingImage)
        screen.show()
    }

    fun isOn() = screen.displayOn
    fun close() = screen.close()

    private lateinit var lastDisplayed: Instant

    fun startTimeAndTempThing(now: Instant) {
        lastDisplayed = now
        screen.displayOn = true
        sleeping = false
        displayCurrentStatus(now)
    }

    private const val ALERT = "ALERT!!!"

    /**
     * Display the time and temp on the OLED display. Checks a timer to see if the display has been on for a defined
     * period of time.
     */
    fun displayCurrentStatus(now: Instant) = with(screenGraphics) {
        if (shouldBlank(now)) return

        // if "not alerting"
        if (!StatusFlags.collisionDetected.get()) {
            color = Color.BLACK
            fillRect(0, 0, 128, 128)
            drawImage(okStatusImage, 34, 0, 50, 50, null)
            color = Color.WHITE
            font = timeFont
            // the time, please?
            val time = LocalTime.now().let { String.format("%2d:%02d:%02d", it.hour, it.minute, it.second) }
            drawString(time, timeFontMetrics.center(time), 100)
        } else {
            drawImage(alertStatusImage, 0, 0, 127, 127, null)
//            color = Color.RED
//            // TODO this should be "static"
//            drawString(
//                ALERT,
//                distanceFontMetrics.center(ALERT),
//                distanceFontMetrics.height + 1
//            )
            color = Color.BLACK
            val text = String.format("D: %5.1f cm", StatusFlags.sonarScan.get().range)
            drawString(text, distanceFontMetrics.center(text), 120 - distanceFontMetrics.descent)
        }

        screen.display(image)
        screen.show()
    }

    private fun loadImage(name: String) = ImageIO.read(this::class.java.getResourceAsStream(name))

    private fun shouldBlank(now: Instant): Boolean {
        // check the timer - turn the screen off if on too long
        if (screen.displayOn) {
            val howLong = Duration.between(lastDisplayed, now).toSeconds()
            // turn it off
            if (howLong >= MAX_TIME) {
                screen.displayOn = false
                sleeping = false
            } else if (!sleeping && howLong >= MAX_SLEEP) {
                // screen remains on for now, but we return false to prevent over-writing
                // TODO this is awkward
                sleeping = true
                screen.display(sleepingImage)
                screen.show()
            }
        }
        return sleeping || !screen.displayOn
    }

    private fun FontMetrics.center(text: String) = max((128 - stringWidth(text)) / 2, 0)

    const val MAX_TIME = 60
    const val MAX_SLEEP = 30
}
