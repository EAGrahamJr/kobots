/*
 * Copyright 2022-2024 by E. A. Graham, Jr.
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

package crackers.kobots.app.enviro

import com.diozero.devices.oled.MonochromeSsdOled.Height
import com.diozero.devices.oled.SSD1306
import com.diozero.devices.oled.SsdOledCommunicationChannel
import crackers.kobots.app.AppCommon
import crackers.kobots.app.AppCommon.whileRunning
import crackers.kobots.app.Startable
import crackers.kobots.app.multiplexor
import crackers.kobots.parts.elapsed
import crackers.kobots.parts.scheduleWithFixedDelay
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.time.Instant
import java.time.LocalTime
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds


/**
 * **WAS** 4 digit 14 segment display but the dang thing quit on me.
 */
object DisplayDos : Startable {
    private val logger = LoggerFactory.getLogger("DisplayDos")
    private val screen = run {
        val i2c = multiplexor.getI2CDevice(1, SSD1306.DEFAULT_I2C_ADDRESS)
        val channel = SsdOledCommunicationChannel.I2cCommunicationChannel(i2c)
        SSD1306(channel, Height.SHORT).apply {
            setContrast(0x20)
            setDisplayOn(false)
        }
    }

    private val graphics: Graphics2D
    private val lcdFontOffset: Int
    private val lcdFont: Font
    private val image = BufferedImage(SSD1306.DEFAULT_WIDTH, Height.SHORT.lines, screen.nativeImageType).also {
        val f = Font.createFont(Font.TRUETYPE_FONT, this.javaClass.getResourceAsStream("/lcd.ttf")).deriveFont(12f)
        graphics = (it.graphics as Graphics2D).apply {
            background = Color.BLACK
            color = Color.WHITE
            lcdFont = fitFont(f)
            lcdFontOffset = getFontMetrics(lcdFont).height
        }
    }

    private fun Graphics2D.fitFont(f: Font): Font {
        val nextFont = f.deriveFont(f.size + .5f)
        val fm = getFontMetrics(nextFont)
        // note: this is adjusted to fit the specfic font above, so be careful
        return if (fm.height > Height.SHORT.lines) f.also {
            logger.debug("Final height is ${fm.height}")
        } else fitFont(nextFont)
    }

    private lateinit var cluckFuture: Future<*>

    private enum class Mode {
        IDLE, CLUCK, TEXT
    }

    private val mode = AtomicReference(Mode.IDLE)

    lateinit var timerStart: Instant
    private var currentMode: Mode
        get() = mode.get()
        set(m) {
            logger.debug("Mode changed to {}", m)
            mode.set(m)
            timerStart = Instant.now()
            screen.setDisplayOn(m != Mode.IDLE)
        }

    private val TEXT_EXPIRES = java.time.Duration.ofSeconds(30)
    private val CLUCK_EXPIRES = java.time.Duration.ofSeconds(5)
    private val TIME_EXPIRES = java.time.Duration.ofSeconds(30)

    override fun start() {
        clear()
        var colon = true

        cluckFuture = AppCommon.executor.scheduleWithFixedDelay(1.seconds, 1.seconds) {
            whileRunning {
                val now = LocalTime.now()
                when (currentMode) {
                    // flip to cluck mode every 5 minutes
                    Mode.IDLE -> if (now.minute % 5 == 0) {
                        currentMode = Mode.CLUCK
                        graphics.printLcd("Cluck")
                    }

                    // if text mode, assume the display contains the requested text already
                    Mode.TEXT -> {
                        if (timerStart.elapsed() > TEXT_EXPIRES) currentMode = Mode.IDLE
                        // TODO initiate a scroll?
                    }

                    Mode.CLUCK -> {
                        if (timerStart.elapsed() > TIME_EXPIRES) currentMode = Mode.IDLE
                        else if (timerStart.elapsed() > CLUCK_EXPIRES) {
                            val timeString = if (colon) "%2d:%02d" else "%2d %02d"
                            graphics.printLcd(String.format(timeString, now.hour, now.minute))
                            colon = !colon
                        }
                    }
                }
            }
        }
    }

    fun text(s: String) {
        if (s.isBlank()) currentMode = Mode.IDLE
        else {
            currentMode = Mode.TEXT
            graphics.printLcd(s)
        }
    }

    fun cluck() {
        if (currentMode == Mode.IDLE) currentMode = Mode.CLUCK
    }

    private fun Graphics2D.printLcd(s: String) {
        clearRect(0, 0, image.width, image.height)
        font = lcdFont
        drawString(s, 0, lcdFontOffset)
        screen.display(image)
    }

    private fun clear() {
        graphics.clearRect(0, 0, image.width, image.height)
        screen.display(image)
    }


    override fun stop() {
        screen.setDisplayOn(false)
    }
}
