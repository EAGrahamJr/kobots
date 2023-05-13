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

import com.diozero.api.I2CDevice
import com.diozero.devices.oled.SSD1306
import com.diozero.devices.oled.SsdOledCommunicationChannel
import crackers.kobots.utilities.loadImage
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.Instant

/**
 * Super tiny screen!
 */
internal object SmallScreen : BBScreen {
    private val screenGraphics: Graphics2D
    private val image: BufferedImage
    private val menuFont = Font(Font.SANS_SERIF, Font.PLAIN, 12)
    private val menuFontMetrics: FontMetrics
    private val menuLineHeight: Int
    private val BACKGROUND = Color.BLACK
    private val FOREGROUND = Color.WHITE

    private val screen by lazy {
        val channel = SsdOledCommunicationChannel.I2cCommunicationChannel(I2CDevice(1, SSD1306.DEFAULT_I2C_ADDRESS))
        SSD1306(channel, SSD1306.Height.SHORT)
    }
    private var lastDisplayed = Instant.now()
    const val MAX_TIME = 30

    init {
        image = BufferedImage(128, 32, BufferedImage.TYPE_BYTE_GRAY).also {
            screenGraphics = it.graphics as Graphics2D
        }

        screen.clear()
        screen.setDisplayOn(true)
        screen.setContrast(0x20.toByte())

        with(screenGraphics) {
            menuFontMetrics = getFontMetrics(menuFont)
            menuLineHeight = menuFontMetrics.ascent + 1
        }
    }

    private fun Graphics2D.clearScreen() {
        color = BACKGROUND
        fillRect(0, 0, 127, 127)
    }

    private var _displayOn = true
    override var on: Boolean
        get() = _displayOn
        set(b) {
            _displayOn = b
            screen.setDisplayOn(b)
        }

    override fun close() = screen.close()

    /**
     * Dumb thing for the fun of it
     */
    override fun startupSequence() {
        with(screenGraphics) {
            clearScreen()
            color = FOREGROUND
            font = menuFont
            val okStatusImage = screen.scaleImage(loadImage("/smiley.png"))
            drawImage(okStatusImage, 0, 0, null)
        }

        lastDisplayed = Instant.now()
    }

    /**
     * Display the application statuses. Checks to see how long the display has been on and will shut it off after a
     * period of time
     */
    override fun execute(buttonsPressed: Boolean, currentMenu: List<Menu.MenuItem>) = with(screenGraphics) {
        val now = Instant.now()

        if (buttonsPressed) {
            lastDisplayed = now
            on = true
            lastMenu = null
        }

        // check the timer - turn the screen off if on too long
        if (on) {
            Duration.between(lastDisplayed, now).toSeconds().toInt().also { elapsed ->
                if (elapsed >= MAX_TIME) {
                    on = false
                } else {
                    showMenu(currentMenu)
                    screen.display(image)
                }
            }
        }
    }

    private fun lineOffset(line: Int) = (line * menuLineHeight) + menuFontMetrics.ascent
    private fun Graphics2D.clearLine(line: Int, start: Int = 0) {
        color = BACKGROUND
        fillRect(start, line * menuLineHeight, 127, menuLineHeight)
    }

//    private fun Graphics2D.showTime() {
//        clearLine(0)
//
//        font = menuFont
//        val time = LocalTime.now().let { String.format("%2d:%02d:%02d", it.hour, it.minute, it.second) }
//        val tx = menuFontMetrics.center(time, 128)
//        color = Color.RED
//        drawString(time, tx, lineOffset(0))
//    }

    private var lastMenu: List<Menu.MenuItem>? = null
    private fun Graphics2D.showMenu(menu: List<Menu.MenuItem>) {
        // don't display it again
        if (menu.isEmpty() || menu == lastMenu) return
        lastMenu = menu
        clearScreen()

        font = menuFont
        var lineCount = 0
        menu.forEachIndexed { index, item ->
            val text = "${index + 1} - ${item.name}"
            if (index % 2 == 0) {
                clearLine(lineCount)
                if (item.name.isNotBlank()) {
                    color = FOREGROUND
                    drawString(text, 0, lineOffset(lineCount))
                }
            } else {
                if (item.name.isNotBlank()) {
                    clearLine(lineCount, 60)
                    color = FOREGROUND
                    drawString(text, 64, lineOffset(lineCount))
                }
                lineCount++
            }
        }
    }
}
