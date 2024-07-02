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
import crackers.kobots.app.enviro.DieAufseherin
import crackers.kobots.app.isDaytime
import crackers.kobots.app.multiplexor
import crackers.kobots.graphics.loadImage
import crackers.kobots.parts.*
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Future
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Shows stuff on a timed basis.
 */
object LargerMonitor : AppCommon.Startable {
    private val logger = LoggerFactory.getLogger("LargerMonitor")
    internal val TURN_OFF = Duration.ofSeconds(15)

    private const val MAX_WD = 128
    private const val MAX_HT = 64
    private val imageType = BufferedImage.TYPE_BYTE_BINARY

    private val screenGraphics: Graphics2D
    private val screenImage = BufferedImage(MAX_WD, MAX_HT, imageType).also { img: BufferedImage ->
        screenGraphics = (img.graphics as Graphics2D).apply {
            background = Color.BLACK
        }
    }

    private fun Graphics2D.clear() = clearRect(0, 0, MAX_WD, MAX_HT)

    private lateinit var screen: SH1106

    private lateinit var future: Future<*>
    private val ball8 = EightBall(screenGraphics, height = MAX_HT)
    private val BALL8EXPIRY = Duration.ofSeconds(25)

//    private enum class Phases {
//        New_moon, Waxing_crescent, First_quarter, Waxing_gibbous, Full_moon, Waning_gibbous, Last_quarter,
//        Waning_crescent
//    }

    private enum class Mode { OFF, BALLER, MONITOR }

    override fun start() {
        screen = run {
            val i2CDevice = multiplexor.getI2CDevice(0, SH1106.DEFAULT_I2C_ADDRESS)
            SH1106(SsdOledCommunicationChannel.I2cCommunicationChannel(i2CDevice)).apply { setContrast(.2f) }
        }
        loadImage("/oh-yeah.png").apply {
            screenGraphics.drawImage(this, 0, 0, MAX_WD, MAX_HT, null)
            screen.display = on
            screen.display(screenImage)
        }

        // get ready for drawing our thing(s)
        screenGraphics.clear()
        var monitorTimer = Instant.now()
        var currentMode = Mode.OFF
        var randomIdleTime = Duration.ofMinutes(1)

        fun notBusy() {
            // don't show shit at night
            if (!isDaytime) {
                if (screen.display) screen.display = off
            } else if (screen.display) {
                screen.display = off

                monitorTimer = Instant.now()
                randomIdleTime = Duration.ofMinutes(Random.nextLong(1, 5).also {
                    logger.debug("Screen on -- baller in ${it} min")
                })
            } else {
                // has baller expired? (e.g. time to show
                if (monitorTimer.elapsed() > randomIdleTime) {
                    logger.debug("Not on - time to show")
                    currentMode = Mode.BALLER
                    with(screen) {
                        display = on
                        ball8.image()
                        display(screenImage)
                        5.seconds.sleep()
                        ball8.next()
                        display(screenImage)
                    }
                    monitorTimer = Instant.now()
                }
            }
        }

        fun toMonitor() {
            if (currentMode != Mode.MONITOR) {
                screen.display = on
                DisplayDos.showEyes(DisplayDos.LOOK_LEFT)
            }
            currentMode = Mode.MONITOR
            updateStatusDisplays()
        }

        future = AppCommon.executor.scheduleWithFixedDelay(100.milliseconds, 100.milliseconds) {
            AppCommon.whileRunning {
                when (DieAufseherin.currentMode) {
                    // go away
                    DieAufseherin.SystemMode.SHUTDOWN -> {
                        screen.close()
                    }
                    // display shit when not doing anything
                    DieAufseherin.SystemMode.IDLE -> {
                        when (currentMode) {
                            Mode.OFF -> notBusy()

                            Mode.MONITOR -> {
                                currentMode = Mode.OFF
                                DisplayDos.eyesReset()
                            }

                            Mode.BALLER -> {
                                if (monitorTimer.elapsed() > BALL8EXPIRY) currentMode = Mode.OFF
                            }
                        }
                    }

                    else -> {
                        // TODO something else with the other things?
                    }
//                    DieAufseherin.SystemMode.IN_MOTION -> toMonitor()
//                    DieAufseherin.SystemMode.MANUAL -> toMonitor()

                }
            }
        }
    }

    private fun updateStatusDisplays() {
        screen.display(screenImage)
    }

    override fun stop() {
        if (::future.isInitialized) future.cancel(true)
        if (::screen.isInitialized) screen.close()
    }
}
