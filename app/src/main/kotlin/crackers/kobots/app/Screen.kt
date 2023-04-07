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
import crackers.kobots.utilities.loadImage
import java.awt.*
import java.awt.geom.Arc2D
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import kotlin.math.max
import kotlin.math.roundToInt

object Screen {
    private val screenGraphics: Graphics2D
    private val image: BufferedImage
    private val timeFont = Font("Helvetica", Font.BOLD, 18)
    private val timeFontMetrics: FontMetrics
    private var distanceFont = Font(Font.MONOSPACED, Font.BOLD, 24)
    private var distanceFontMetrics: FontMetrics

    private val screen: GrayOled by lazy { SSD1327(SSD1327.ADAFRUIT_STEMMA) }

    private val okStatusImage = loadImage("/smiley.png")
    private val alertStatusImage = loadImage("/not-smiley.jpg")
    private val sleepingImage = loadImage("/snooze.jpg")
    private var sleeping = false
    const val MAX_TIME = 60
    const val MAX_SLEEP = 30

    private val radarUpperLeft = Point2D.Double(24.0, 78.0)
    private val radarDrawRadius = 40.0
    private val radarArcCenter: Point2D
    private val radarArcShape: Arc2D

    init {
        image = BufferedImage(128, 128, BufferedImage.TYPE_BYTE_GRAY).also {
            screenGraphics = it.graphics as Graphics2D
        }
        timeFontMetrics = screenGraphics.getFontMetrics(timeFont)
        distanceFontMetrics = screenGraphics.getFontMetrics(distanceFont)

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

            // set up the "radar" screen
            // create the pie shape for the radar - starts at 45 degrees and sweeps for 90
            val diameter = 2 * radarDrawRadius // because we actually draw it in a box
            radarArcShape = Arc2D.Double(radarUpperLeft.x, radarUpperLeft.y, diameter, diameter, 45.0, 90.0, Arc2D.PIE)
            radarArcCenter = Point2D.Double(radarUpperLeft.x + radarDrawRadius, radarUpperLeft.y + radarDrawRadius)
        }
    }

    fun isOn() = screen.displayOn
    fun close() = screen.close()

    private var lastDisplayed = Instant.now().minusSeconds(MAX_SLEEP.toLong())

    fun startTimeAndTempThing(now: Instant) {
        lastDisplayed = now
        screen.displayOn = true
        sleeping = false
        with(screenGraphics) {
            // clear radar area
            color = Color.BLACK
            fillRect(22, 22, 120, 120)
        }
        displayCurrentStatus(now)
    }

    private val bogeys = HashMap<Int, Float>()

    /**
     * Display the application statuses. Checks to see how long the display has been on and will shut it off after a
     * period of time
     */
    fun displayCurrentStatus(now: Instant) = with(screenGraphics) {
        showTemperature()
        showTime()

        if (shouldBlank(now)) return

        // clear radar area and draw the "screen"
        color = Color.BLACK
        fill(radarArcShape)

        color = Color.WHITE
        draw(radarArcShape)

        StatusFlags.sonarScan.get()?.also { scan ->
            // if the range is under the radius, store the bogey (otherwise clear)
            if (scan.range < radarDrawRadius) {
                bogeys.put(scan.bearing, scan.range)
            } else {
                bogeys.remove(scan.bearing)
            }

            // negative angle because coordinates are upside down from eyeballs
            radarArcCenter.also { c ->
                // draw the sweep line
                val radians = Math.toRadians(-scan.bearing.toDouble())
                val sweepX = (c.x + radarDrawRadius * Math.cos(radians)).toInt()
                val sweepY = (c.y + radarDrawRadius * Math.sin(radians)).toInt()
                color = Color.GREEN
                drawLine(c.x.toInt(), c.y.toInt(), sweepX, sweepY)

                // draw all the current bogies
                color = Color.RED
                bogeys.forEach { angle, range ->
                    val rads = Math.toRadians(-angle.toDouble())
                    val bx = (c.x + range * Math.cos(rads)).toInt()
                    val by = (c.y + range * Math.sin(rads)).toInt()
                    fillArc(bx, by, 3, 3, 0, 360)
                }
            }
        }

        showIt()
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

    private fun Graphics2D.showTime() {
        color = Color.BLACK
        fillRect(20, 0, 120, 21)
        font = timeFont
        val time = LocalTime.now().let { String.format("%2d:%02d:%02d", it.hour, it.minute, it.second) }
        val tx = 20 + (107 - timeFontMetrics.stringWidth(time)) / 2
        color = Color.CYAN
        drawString(time, tx, timeFontMetrics.ascent + 1)
    }

    private fun shouldBlank(now: Instant): Boolean {
        // check the timer - turn the screen off if on too long
        if (screen.displayOn) {
            if (StatusFlags.scanningFlag.get()) {
                lastDisplayed = now
                return false
            }
            val howLong = Duration.between(lastDisplayed, now).toSeconds()
            // turn it off
            if (howLong >= MAX_TIME) {
                screen.displayOn = false
                sleeping = false
            } else if (!sleeping && howLong >= MAX_SLEEP) {
                // screen remains on for now, but we return false to prevent over-writing
                // TODO this is awkward
                sleeping = true
                displayImage(sleepingImage)
                showIt()
            }
        }
        return sleeping || !screen.displayOn
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
