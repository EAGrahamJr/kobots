/*
 * Copyright 2022-2025 by E. A. Graham, Jr.
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
import com.diozero.devices.oled.SH1106
import com.diozero.devices.oled.SSD1306
import com.diozero.devices.oled.SsdOledCommunicationChannel
import crackers.kobots.app.AppCommon
import crackers.kobots.app.enviro.DieAufseherin
import crackers.kobots.app.isDaytime
import crackers.kobots.app.multiplexor
import crackers.kobots.graphics.animation.MatrixRain
import crackers.kobots.graphics.toRadians
import crackers.kobots.graphics.widgets.VerticalPercentageIndicator
import crackers.kobots.parts.elapsed
import crackers.kobots.parts.off
import crackers.kobots.parts.on
import crackers.kobots.parts.scheduleWithFixedDelay
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Future
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Displays a status (0-100 percent) on a rotated 132x32 display. Or falling "matrix rain".
 */
object VerticalStatusDisplay : AppCommon.Startable {
    private val IMG_WIDTH = MonochromeSsdOled.Height.SHORT.lines
    private val IMG_HEIGHT = MonochromeSsdOled.DEFAULT_WIDTH

    // set up the vertical thingie and then rotate it to display
    private val myGraphics: Graphics2D
    private val myImage =
        BufferedImage(IMG_HEIGHT, IMG_WIDTH, BufferedImage.TYPE_BYTE_BINARY).also { img ->
            val theRotation =
                AffineTransform().apply {
                    rotate(-90.toRadians())
                    translate(-IMG_WIDTH.toDouble(), 0.0)
                }
            myGraphics = (img.graphics as Graphics2D).apply { transform(theRotation) }
        }

    // for statusy stuff
    private val vpd =
        VerticalPercentageIndicator(
            myGraphics,
            Font(Font.SANS_SERIF, Font.BOLD, 10),
            IMG_HEIGHT,
            label = "Pct",
        )

    // pretty stuff
    private val matrixRain =
        MatrixRain(
            myGraphics,
            0,
            0,
            IMG_WIDTH,
            IMG_HEIGHT,
            displayFont = Font(Font.MONOSPACED, Font.PLAIN, 6),
            useBold = true,
            updateSpeed = 120.milliseconds,
            normalColor = Color.WHITE,
        )

    private val MATRIX_RUN_TIME = Duration.ofMinutes(5)
    private val IM_BORED_NOW = Duration.ofMinutes(2) + Duration.ofSeconds(30)

    private lateinit var future: Future<*>
    private lateinit var startTime: Instant
    private lateinit var screen: SSD1306
    private var statusReady = false

    private val logger: Logger by lazy { LoggerFactory.getLogger("Vert") }

    private enum class WhatImDoing {
        STARTUP,
        MATRIX,
        IDLE,
    }

    override fun start() {
        screen =
            run {
                val i2CDevice = multiplexor.getI2CDevice(3, SH1106.DEFAULT_I2C_ADDRESS)
                val channel = SsdOledCommunicationChannel.I2cCommunicationChannel(i2CDevice)
                SSD1306(channel, MonochromeSsdOled.Height.SHORT).apply { setContrast(0x10) }
            }
        var whatImDoing = WhatImDoing.STARTUP

        future =
            AppCommon.executor.scheduleWithFixedDelay(10.seconds, 100.milliseconds) {
                AppCommon.whileRunning {
                    when (DieAufseherin.currentMode) {
                        // if everything is cool, let's do our pretty stuff...
                        DieAufseherin.SystemMode.IDLE -> {
                            when (whatImDoing) {
                                // boread yet?
                                WhatImDoing.IDLE ->
                                    if (isDaytime && startTime.elapsed() > IM_BORED_NOW) {
                                        enterTheMatrix()
                                        whatImDoing = WhatImDoing.MATRIX
                                    }
                                // done yet?
                                WhatImDoing.MATRIX ->
                                    if (startTime.elapsed() > MATRIX_RUN_TIME) {
                                        sleep()
                                        whatImDoing = WhatImDoing.IDLE
                                    }
                                // never started the sequence, so init everything
                                WhatImDoing.STARTUP -> {
                                    enterTheMatrix()
                                    whatImDoing = WhatImDoing.MATRIX
                                }
                            }
                        }

                        else -> {
//                        TODO -- everything else, like vertical percentage of what?
                        }
                    }
                }
            }
    }

    override fun stop() {
        matrixRain.stop()
        if (::future.isInitialized) future.cancel(true)
        if (::screen.isInitialized) screen.close()
    }

    private fun enterTheMatrix() {
        statusReady = false
        if (!screen.display) screen.display = on
        if (!matrixRain.running()) {
            logger.debug("Here we go...")
            matrixRain.start { screen.display(myImage) }
            startTime = Instant.now()
        }
    }

    private fun sleep() {
        if (screen.display) {
            logger.debug("Sleepy time")
            matrixRain.stop()
            statusReady = false
            screen.display = off
            startTime = Instant.now()
        }
    }

    // update status display
    private fun update(statusValue: Int) {
        if (!statusReady) {
            vpd.drawStatic()
            statusReady = true
        }
        if (!screen.display) screen.display = on

        vpd.updateValue(statusValue)
        screen.display(myImage)
    }
}
