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

package crackers.kobots.app.display

import com.diozero.devices.oled.MonochromeSsdOled
import com.diozero.devices.oled.MonochromeSsdOled.Height
import com.diozero.devices.oled.SSD1306
import com.diozero.devices.oled.SsdOledCommunicationChannel
import crackers.kobots.app.AppCommon
import crackers.kobots.app.AppCommon.whileRunning
import crackers.kobots.app.multiplexor
import crackers.kobots.graphics.animation.*
import crackers.kobots.parts.app.KobotSleep
import crackers.kobots.parts.elapsed
import crackers.kobots.parts.fitFont
import crackers.kobots.parts.scheduleWithFixedDelay
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Point
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds

/**
 * Shows text and time in a multi-segment like font, as well as eyes.
 */
object DisplayDos : AppCommon.Startable {

    private val logger = LoggerFactory.getLogger("DisplayDos")
    private lateinit var dosScreen: SSD1306
    private val MAX_WD = MonochromeSsdOled.DEFAULT_WIDTH
    private val MAX_HT = Height.SHORT.lines

    private val graphics: Graphics2D
    private val lcdFontOffset: Int
    private val lcdFont: Font
    private val image = BufferedImage(MAX_WD, MAX_HT, BufferedImage.TYPE_BYTE_BINARY).also {
        val f = Font.createFont(Font.TRUETYPE_FONT, this.javaClass.getResourceAsStream("/lcd.ttf")).deriveFont(12f)
        graphics = (it.graphics as Graphics2D).apply {
            background = Color.BLACK
            color = Color.WHITE
            lcdFont = fitFont(f, Height.SHORT.lines)
            lcdFontOffset = getFontMetrics(lcdFont).height
        }
    }

    private lateinit var cluckFuture: Future<*>

    private enum class Mode {
        IDLE, CLUCK, TEXT, EYES, RANDOM
    }

    private val mode = AtomicReference(Mode.IDLE)

    private lateinit var timerStart: Instant
    private var currentMode: Mode
        get() = mode.get()
        set(m) {
            if (mode.get() != m) {
                logger.debug("Mode changed to {}", m)
                mode.set(m)
                timerStart = Instant.now()
                dosScreen.setDisplayOn(m != Mode.IDLE)
            }
        }

    private val TEXT_EXPIRES = Duration.ofSeconds(30)
    private val CLUCK_EXPIRES = Duration.ofSeconds(5)
    private val TIME_EXPIRES = Duration.ofSeconds(30)
    private val EYES_EXPIRES = Duration.ofMinutes(1)

    private var eyesLastChanged = Instant.EPOCH
    private val currentExpression = AtomicReference<Expression>()

    override fun start() {
        dosScreen = run {
            val i2c = multiplexor.getI2CDevice(1, SSD1306.DEFAULT_I2C_ADDRESS)
            val channel = SsdOledCommunicationChannel.I2cCommunicationChannel(i2c)
            SSD1306(channel, Height.SHORT).apply {
                setContrast(0x20)
                setDisplayOn(false)
            }
        }
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
                        if (timerStart.elapsed() > TIME_EXPIRES) {
                            currentMode = Mode.IDLE
                        } else if (timerStart.elapsed() > CLUCK_EXPIRES) {
                            val timeString = if (colon) "%2d:%02d" else "%2d %02d"
                            graphics.printLcd(String.format(timeString, now.hour, now.minute))
                            colon = !colon
                        }
                    }

                    Mode.EYES -> {
                        blink(currentExpression.get())
                    }

                    Mode.RANDOM -> {
                        if (timerStart.elapsed() > EYES_EXPIRES) {
                            currentMode = Mode.IDLE
                        } // choose a random eye after 10 seconds
                        else {
                            val random = (CannedExpressions.entries - CannedExpressions.CLOSED).random().expression
                            blink(random)
                        }
                    }
                }
            }
        }
    }

    fun text(s: String) {
        if (s.isBlank()) {
            currentMode = Mode.IDLE
        } else if (currentMode == Mode.IDLE) {
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
        color = Color.WHITE
        drawString(s, 0, lcdFontOffset)
        dosScreen.display(image)
    }

    private fun clear() {
        graphics.clearRect(0, 0, image.width, image.height)
        dosScreen.display(image)
    }

    override fun stop() {
        if (::dosScreen.isInitialized) dosScreen.close()
    }

    // EYE STUFF ------------------------------------------------------------------------------------------------------
    val LOOK_RIGHT = Expression(
        lidPosition = Eye.LidPosition.ONE_QUARTER,
        pupilPosition = Pupil.Position.RIGHT + Pupil.Position.CENTER
    )
    val LOOK_LEFT = Expression(
        lidPosition = Eye.LidPosition.ONE_QUARTER,
        pupilPosition = Pupil.Position.LEFT + Pupil.Position.CENTER
    )

    // show a random image
    private val CHANGE_EYES = Duration.ofSeconds(5)

    private val leftEye = Eye(Point(46, 15), 15)
    private val rightEye = Eye(Point(82, 15), 15)
    private val eyes = PairOfEyes(leftEye, rightEye)

    /**
     * Show this expression
     */
    fun showEyes(expression: Expression) {
        currentExpression.set(expression)
        currentMode = Mode.EYES
    }

    private fun blink(expression: Expression) {
        if (eyesLastChanged.elapsed() > CHANGE_EYES) {
            // do a blink
            graphics.clearRect(0, 0, MAX_WD, MAX_HT)
            drawEyes(CannedExpressions.CLOSED.expression)
            KobotSleep.millis(200)
            drawEyes(expression)
            eyesLastChanged = Instant.now()
        }
    }

    private fun drawEyes(expression: Expression) {
        eyes(expression)
        eyes.draw(graphics)
        dosScreen.display(image)
    }

    /**
     * Pick a random one for a while
     */
    fun randomEye() {
        if (currentMode == Mode.IDLE) currentMode = Mode.RANDOM
    }

    internal fun eyesReset() {
        if (currentMode == Mode.EYES) currentMode = Mode.RANDOM
    }
}
