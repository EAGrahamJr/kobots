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

import com.diozero.devices.oled.SH1106
import com.diozero.devices.oled.SsdOledCommunicationChannel
import crackers.kobots.app.AppCommon
import crackers.kobots.app.Jimmy
import crackers.kobots.app.enviro.DieAufseherin
import crackers.kobots.app.multiplexor
import crackers.kobots.graphics.loadImage
import crackers.kobots.graphics.widgets.DirectionPointer
import crackers.kobots.parts.elapsed
import crackers.kobots.parts.scheduleWithFixedDelay
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Future
import kotlin.time.Duration.Companion.milliseconds

/**
 * Shows where the arm is on a timed basis.
 */
object MonitorBank : AppCommon.Startable {
    private val logger = LoggerFactory.getLogger("MonitorBank")
    internal val TURN_OFF = Duration.ofSeconds(15)

    private const val MAX_WD = 128
    private const val MAX_HT = 32
    private val imageType = BufferedImage.TYPE_BYTE_BINARY

    private val screenGraphics: Graphics2D
    private val screenImage = BufferedImage(MAX_WD, MAX_HT * 2, imageType).also { img: BufferedImage ->
        screenGraphics = (img.graphics as Graphics2D).apply {
            background = Color.BLACK
        }
    }
    private val SMALL_FONT = Font(Font.SANS_SERIF, Font.BOLD, 14)
    private val swipePointer = DirectionPointer(
        screenGraphics,
        SMALL_FONT,
        64,
        clockWise = false,
        endAngle = 90,
        label = "SWIPE!"
    )

    private lateinit var screen: SH1106

    private lateinit var future: Future<*>
    private var screenOnAt = Instant.EPOCH

    override fun start() {
        screen = run {
            val i2CDevice = multiplexor.getI2CDevice(0, SH1106.DEFAULT_I2C_ADDRESS)
            SH1106(SsdOledCommunicationChannel.I2cCommunicationChannel(i2CDevice)).apply { setContrast(0f) }
        }
        loadImage("/oh-yeah.png").apply {
            screenGraphics.drawImage(this, 0, 0, 123, 63, null)
            screen.setDisplayOn(true)
            screen.display(screenImage)
        }

        screenOnAt = Instant.now()
        var screenOn = true
        // get ready for drawing our thing(s)
        screenGraphics.clearRect(0, 0, MAX_WD, MAX_HT)
        swipePointer.drawStatic()

        future = AppCommon.executor.scheduleWithFixedDelay(100.milliseconds, 100.milliseconds) {
            AppCommon.whileRunning {
                val screenDirty = when (DieAufseherin.currentMode) {
                    DieAufseherin.SystemMode.IDLE -> {
                        false
                    }

                    DieAufseherin.SystemMode.IN_MOTION -> {
                        DisplayDos.showEyes(DisplayDos.LOOK_LEFT)
                        true
                    }

                    DieAufseherin.SystemMode.MANUAL -> {
                        DisplayDos.showEyes(DisplayDos.LOOK_LEFT)
                        true
                    }

                    DieAufseherin.SystemMode.SHUTDOWN -> {
                        false
                    }
                }

                if (screenDirty) {
                    screenOnAt = Instant.now()
                    if (!screenOn) {
                        screen.setDisplayOn(true)
                        screenOn = true
                    }

                    updateStatusDisplays()
                    screen.display(screenImage)
                } else {
                    if (screenOnAt.elapsed() > TURN_OFF && screenOn) {
                        DisplayDos.eyesReset()
                        screen.setDisplayOn(false)
                        LiftStatusDisplay.sleep()
                        screenOn = false
                    }
                }
            }
        }
    }

    private fun updateStatusDisplays() {
        LiftStatusDisplay.update(Jimmy.sunAzimuth.current())
        swipePointer.updateValue(Jimmy.sunElevation.current())
    }

    override fun stop() {
        LiftStatusDisplay.close()

        if (::future.isInitialized) future.cancel(true)
        if (::screen.isInitialized) screen.close()
    }
}
