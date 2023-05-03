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

package dork

import base.MARVIN
import base.REMOTE_PI
import base.minutes
import crackers.kobots.devices.display.SSD1327
import device.examples.qwiic.oled.loadImage
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.geom.Arc2D
import java.awt.geom.Arc2D.PIE
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.lang.Thread.sleep
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlin.random.Random.Default.nextInt

val TIME_RECT = Rectangle2D.Double(20.0, 0.0, 128.0, 21.0)
private val timeFont = Font("Helvetica", Font.BOLD, 18)
private lateinit var timeFontMetrics: FontMetrics

private val MAX = 127

fun main() {
    System.setProperty(REMOTE_PI, MARVIN)

    val sleepingImage = loadImage("/snooze.jpg")

    val screenGraphics: Graphics2D
    val screen = SSD1327(SSD1327.ADAFRUIT_STEMMA)
    val image = BufferedImage(128, 128, BufferedImage.TYPE_BYTE_GRAY).also {
        screenGraphics = it.graphics as Graphics2D
    }
    timeFontMetrics = screenGraphics.getFontMetrics(timeFont)

    var fm: FontMetrics // because it will be used over and over
    var fh: Int // font height

    screen.clear()
    with(screenGraphics) {
        // draw thermometer box - this occupies the full height over to pixel 20
        showTemperature()

        drawImage(sleepingImage, 22, 22, 106, 106, null)
        screen.display(image)
        screen.show()
        sleep(5000)
//
        // remaining space starts at 20,22 - if we do 22, 22 then it's square
        val r = Rectangle2D.Double(22.0, 22.0, 120.0, 120.0) // too large but don't care
        color = Color.BLACK
        fill(r)

        val upperLeft = Point2D.Double(24.0, 78.0)
        val radius = 40.0

        // create the pie shape for the radar - starts at 45 degrees and sweeps for 90
        // because of the squishing (not square), add some more degrees to match the line drawing below
        val arc = Arc2D.Double(upperLeft.x, upperLeft.y, 2 * radius, 2 * radius, 45.0, 90.0, PIE)
        val arcCenter = Point2D.Double(upperLeft.x + radius, upperLeft.y + radius)

        var sweepAngle = 45.0
        var direction = true
        1 minutes {
            showTime()

            color = Color.BLACK
            fill(arc)

            color = Color.WHITE
            draw(arc)
            val foo = if (nextInt(1, 10) > 5) nextInt(1, 40) else null
            drawSweep(sweepAngle, radius, arcCenter, foo)

            // iterating for display
            sweepAngle += if (direction) 5.0 else -5.0
            sweepAngle = when {
                sweepAngle > 135 -> {
                    direction = false
                    135.0
                }

                sweepAngle < 45 -> {
                    direction = true
                    45.0
                }

                else -> sweepAngle
            }

            screen.display(image)
            screen.show()
            sleep(200)
        }
    }
    screen.displayOn = false
}

private fun Graphics2D.drawSweep(
    angle: Double,
    radius: Double,
    arcCenter: Point2D.Double,
    range: Int? = null
) {
    // now literally flip the angle to determine where to draw the line
    val radians = Math.toRadians(-angle)
    with(arcCenter) {
        val x2 = (x + radius * Math.cos(radians)).toInt()
        val y2 = (y + radius * Math.sin(radians)).toInt()
        color = Color.GREEN
//        drawLine(x.toInt(), y.toInt(), x2, y2)

        range?.also { r ->
            val sx = (x + r * Math.cos(radians)).toInt()
            val sy = (y + r * Math.sin(radians)).toInt()

            color = Color.RED
            fillArc(sx, sy, 3, 3, 0, 360)
        }
    }
}

private fun Graphics2D.showTemperature() {
    color = Color.WHITE
    font = Font(Font.SANS_SERIF, Font.PLAIN, 10)
    val fh1 = getFontMetrics(font).ascent
    drawString("80", 6, fh1 + 1)
    drawString("40", 6, 126)
    drawString("\u2103", 6, 63 + fh1 / 2)

    // TODO bogus temperature
    color = Color.GRAY
    drawRect(0, 0, 4, 127)
    val temperature = getTemperature()
    color = when {
        temperature < 55 -> Color.GREEN
        temperature < 70 -> Color.YELLOW
        else -> Color.RED
    }

    val diff = ((temperature - 40) * 127 / 40).roundToInt()
    fillRect(0, 127 - diff, 4, 127)
}

fun Graphics2D.showTime() {
    color = Color.BLACK
    fillRect(20, 0, 120, 21)
    font = timeFont
    val time = LocalTime.now().let { String.format("%2d:%02d:%02d", it.hour, it.minute, it.second) }
    val tx = 20 + (107 - timeFontMetrics.stringWidth(time)) / 2
    color = Color.CYAN
    drawString(time, tx, timeFontMetrics.ascent + 1)
}

fun getTemperature() = 50f
