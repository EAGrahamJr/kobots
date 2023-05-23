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

import crackers.kobots.devices.display.SSD1327
import crackers.kobots.utilities.elapsed
import crackers.kobots.utilities.loadImage
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.Instant

/**
 * Return of the OLED!
 */
object TheScreen : AutoCloseable {
    private val screenGraphics: Graphics2D
    private val screehHeight: Int
    private val screenWidth: Int
    private val screen = SSD1327(SSD1327.ADAFRUIT_STEMMA).apply {
        clear()
        screenWidth = 128
        screehHeight = 128
    }
    private val image = BufferedImage(screenWidth, screehHeight, BufferedImage.TYPE_BYTE_GRAY).also {
        screenGraphics = it.graphics as Graphics2D
    }
    private val sleeping by lazy { loadImage("/snooze.jpg") }
    private val okImage by lazy { loadImage("/smiley.png") }
    private val scanImage by lazy { loadImage("/not-smiley.jpg") }
    private val triggeredImage by lazy { loadImage("/screaming.png") }

    //    private var sleepTimer: Instant? = null
    private var lastImage: BufferedImage? = null
    private var sleepTimer: Instant? = null
    private val SLEEP_TIME = Duration.ofMinutes(2)

    fun start() {
        checkRun(10) { execute() }
    }

    private fun execute() {
        when (TheArm.state) {
            TheArm.State.BUSY -> okImage.displayThis()

            TheArm.State.REST -> {
                if (sleepTimer == null) sleepTimer = Instant.now()
                sleeping.displayThis()
            }

            TheArm.State.FRONT -> scanImage.displayThis()
            TheArm.State.GUARDING -> TODO()
            TheArm.State.CALIBRATION -> {
                sleepTimer = null
                showCalibration()
            }
        }
    }

    private fun BufferedImage.displayThis() {
        // proximity alert over-rides image
        val nextImage = if (ControlThing.tooClose) triggeredImage else this

        if (nextImage != lastImage) {
            sleepTimer = null
            lastImage = nextImage
            try {
                screen.displayOn = true
                screenGraphics.drawImage(nextImage, 0, 0, screenWidth, screehHeight, null)
                screen.display(image)
                screen.show()
            } catch (e: Exception) {
                println(e.localizedMessage)
                lastImage = null
            }
        } else if (sleepTimer != null) {
            if (sleepTimer!!.elapsed() > SLEEP_TIME) {
                screen.displayOn = false
                sleepTimer = null
            }
        }
    }

    private const val FONT_SIZE = 12
    private fun showCalibration() {
        with(screenGraphics) {
            color = Color.BLACK
            fillRect(0, 0, screenWidth, screehHeight)
            font = Font(Font.SANS_SERIF, Font.PLAIN, FONT_SIZE)
            color = Color.WHITE
            (TheArm.isAt() + "").forEachIndexed { index, position ->
                val stringToDraw =
                    when (index) {
                        0 -> "Waist: ${position}"
                        1 -> "Shoulder: $position"
                        2 -> "Elbow: $position"
                        3 -> "Gripper: open = $position"
                        4 -> "Exit"
                        else -> TODO()
                    }
                val prefix = if (index == TheArm.currentCalirationItem.get()) "*" else " "
                drawString(prefix + stringToDraw, 0, index * fontMetrics.height + fontMetrics.ascent)
            }
            font = Font(Font.MONOSPACED, Font.PLAIN, 8)
            drawString("A:Next B:Back/No C:Forward:Yes", 0, 127)
        }
        screen.display(image)
        screen.show()
    }

    override fun close() {
        screen.close()
    }
}
