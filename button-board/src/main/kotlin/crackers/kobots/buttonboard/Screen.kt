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

package crackers.kobots.buttonboard

import com.diozero.util.SleepUtil
import crackers.kobots.devices.display.GrayOled
import crackers.kobots.devices.display.SSD1327
import crackers.kobots.utilities.center
import crackers.kobots.utilities.loadImage
import java.awt.*
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

/**
 * Displays some system info for a while, before sleeping, and then going dark.
 */
object Screen {
    private val screenGraphics: Graphics2D
    private val image: BufferedImage
    private val menuFont = Font("Helvetica", Font.BOLD, 16)
    private val menuFontMetrics: FontMetrics
    private val menuLineHeight: Int
    private val BACKGROUND = Color.BLACK
    private val FOREGROUND = Color.WHITE

    private val screen: GrayOled by lazy { SSD1327(SSD1327.ADAFRUIT_STEMMA) }

    private val okStatusImage = loadImage("/smiley.png")
    private val alertStatusImage = loadImage("/not-smiley.jpg")
    private val sleepingImage = loadImage("/snooze.jpg")
    private var sleeping = false
    private var lastDisplayed = Instant.now()
    const val MAX_TIME = 60
    const val MAX_SLEEP = 30

    init {
        image = BufferedImage(128, 128, BufferedImage.TYPE_BYTE_GRAY).also {
            screenGraphics = it.graphics as Graphics2D
        }

        screen.clear()
        screen.displayOn = true
        with(screenGraphics) {
            menuFontMetrics = getFontMetrics(menuFont)
            menuLineHeight = menuFontMetrics.height + 2
        }
    }

    private fun Graphics2D.clearScreen() {
        color = BACKGROUND
        fillRect(0, 0, 127, 127)
    }

    fun isOn() = screen.displayOn
    fun close() = screen.close()

    /**
     * Dumb thing for the fun of it
     */
    fun startupSequence() {
        with(screenGraphics) {
            clearScreen()
            color = FOREGROUND
            font = menuFont

            var x = menuFontMetrics.center("Please", 128)
            drawString("Please", x, lineOffset(2))
            showIt()

            // loop a bit for fun
            x = menuFontMetrics.center("Stand-by!", 128)
            val y = lineOffset(3)
            (1..5).forEach {
                clearLine(3)
                showIt()
                SleepUtil.busySleep(Duration.ofSeconds(1).toNanos())
                color = Color.RED
                drawString("Stand-by!", x, y)
                showIt()
                SleepUtil.busySleep(Duration.ofSeconds(1).toNanos())
            }
        }
        displayImage(loadImage("/color-test-image.jpg"))
        showIt()
        SleepUtil.busySleep(Duration.ofSeconds(5).toNanos())
        displayImage(okStatusImage)

        lastDisplayed = Instant.now()
        sleeping = false
    }

    /**
     * Indicates that the display should be on and displaying state information
     */
    fun enableDisplay() {
        lastDisplayed = Instant.now()
        screen.displayOn = true
        sleeping = false
        screenGraphics.clearScreen()
    }

    /**
     * Display the application statuses. Checks to see how long the display has been on and will shut it off after a
     * period of time
     */
    fun execute() = with(screenGraphics) {
        val now = Instant.now()

        // check the timer - turn the screen off if on too long
        if (screen.displayOn) {
            val howLong = Duration.between(lastDisplayed, now).toSeconds()

            when {
                howLong >= MAX_TIME -> {
                    screen.displayOn = false
                    sleeping = false
                }

                howLong >= MAX_SLEEP -> {
                    // screen remains on for now, but we don't actually want to update it again
                    if (!sleeping) {
                        sleeping = true
                        displayImage(sleepingImage)
                        showIt()
                    }
                }

                else -> {
                    showTime()
                    showMenu()
                    showIt()
                }
            }
        }
    }

    private fun lineOffset(line: Int) = (line * menuLineHeight) + menuFontMetrics.ascent
    private fun Graphics2D.clearLine(line: Int) {
        color = BACKGROUND
        fillRect(0, line * menuLineHeight, 127, menuLineHeight)
    }

    private fun Graphics2D.showTime() {
        clearLine(0)

        font = menuFont
        val time = LocalTime.now().let { String.format("%2d:%02d:%02d", it.hour, it.minute, it.second) }
        val tx = menuFontMetrics.center(time, 128)
        color = Color.RED
        drawString(time, tx, lineOffset(0))
    }

    private var lastMenu: List<String>? = null
    private fun Graphics2D.showMenu() {
        Menu.currentMenu.get().also { menu ->
            // don't display it again
            if (menu == lastMenu) return
            lastMenu = menu

            menu.forEachIndexed { index, item ->
                font = menuFont

                val line = index + 1
                clearLine(line)
                if (item.isNotBlank()) {
                    val text = "$line - $item"
                    color = FOREGROUND
                    drawString(text, 0, lineOffset(line))
                }
            }
        }
    }

    /**
     * Display the current image buffer.
     */
    private fun showIt() {
        screen.display(image)
        screen.show()
    }

    private fun displayImage(image: Image) {
        screenGraphics.drawImage(image, 0, 0, 128, 128, null)
    }
}
