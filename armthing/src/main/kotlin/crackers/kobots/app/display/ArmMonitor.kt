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
import crackers.kobots.app.Startable
import crackers.kobots.app.arm.ManualController
import crackers.kobots.app.arm.TheArm
import crackers.kobots.app.enviro.DieAufseherin
import crackers.kobots.app.multiplexor
import crackers.kobots.graphics.animation.KobotRadar
import crackers.kobots.graphics.animation.SimpleRadar
import crackers.kobots.parts.app.io.StatusColumnDelegate
import crackers.kobots.parts.app.io.StatusColumnDisplay
import crackers.kobots.parts.elapsed
import crackers.kobots.parts.scheduleWithFixedDelay
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Point
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Future
import kotlin.time.Duration.Companion.milliseconds

private const val MAX_WD = 128
private const val MAX_HT = 32

/**
 * Shows where the arm is on a timed basis.
 */
object ArmMonitor :
    Startable,
    StatusColumnDisplay by StatusColumnDelegate(MAX_WD, MAX_HT),
    KobotRadar by SimpleRadar(
        Point(0, 0),
        MAX_HT.toDouble()
    ) {
    private val logger = LoggerFactory.getLogger("ArmMonitor")
    internal val TURN_OFF = Duration.ofMinutes(2)

    private val imageType = BufferedImage.TYPE_BYTE_BINARY

    private val statsGraphics: Graphics2D
    private val statsImage = BufferedImage(MAX_WD, MAX_HT, imageType).also { img: BufferedImage ->
        statsGraphics = (img.graphics as Graphics2D).apply {
            font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        }
    }

    private val screenGraphics: Graphics2D
    private val screenImage = BufferedImage(MAX_WD, MAX_HT * 2, imageType).also { img: BufferedImage ->
        screenGraphics = (img.graphics as Graphics2D).apply {
            background = Color.BLACK
        }
    }

    private val screen by lazy {
//        val i2CDevice = I2CDevice(1, SSD1306.DEFAULT_I2C_ADDRESS)
        val i2CDevice = multiplexor.getI2CDevice(0, SH1106.DEFAULT_I2C_ADDRESS)
        SH1106(SsdOledCommunicationChannel.I2cCommunicationChannel(i2CDevice)).apply { setContrast(0f) }
    }

    private lateinit var future: Future<*>

    private var screenOnAt = Instant.EPOCH

    override fun start() {
        screenOnAt = Instant.now()
        var screenOn = true
        future = AppCommon.executor.scheduleWithFixedDelay(100.milliseconds, 100.milliseconds) {
            AppCommon.whileRunning {
                var drawStats = false
                val screenDirty = when (DieAufseherin.currentMode) {
                    DieAufseherin.SystemMode.IDLE -> {
                        ExtenderStatus.sleep()
                        false
                    }

                    DieAufseherin.SystemMode.IN_MOTION -> {
                        drawStats = true
                        statsGraphics.displayStatuses(TheArm.state.position.mapped())
                        ExtenderStatus.update(TheArm.extender.current())
                        DisplayDos.showEyes(DisplayDos.LOOK_LEFT)
                        true
                    }

                    DieAufseherin.SystemMode.MANUAL -> {
                        // screen is always dirty amd enabled
                        screenOnAt = Instant.now()
                        statsGraphics.displayStatuses(ManualController.statuses())
                        ExtenderStatus.update(TheArm.extender.current())
                        DisplayDos.showEyes(DisplayDos.LOOK_LEFT)
                        drawStats = true
                        true
                    }

                    DieAufseherin.SystemMode.SHUTDOWN -> {
                        stop()
                        false
                    }
                }

                if (screenDirty) {
                    if (!screenOn) {
                        screen.setDisplayOn(true)
                        screenOn = true
                    }

                    screenGraphics.clearRect(0, 0, MAX_WD, 2 * MAX_HT)

                    if (drawStats) screenGraphics.drawImage(statsImage, 0, MAX_HT, null)
                    screen.display(screenImage)
                } else {
                    if (screenOnAt.elapsed() > TURN_OFF && screenOn) {
                        screen.setDisplayOn(false)
                        screenOn = false
                    }
                    if (screenOn) DisplayDos.eyesReset()
                }
            }
        }
    }

    fun ping(angle: Int, distance: Float) {
        // TODO make this not so hard-waired
        // scale the distance based on percentage of max (25.5)
        val scaled = MAX_HT * (distance / 25.5f)
        updateScan(KobotRadar.RadarScan(angle + 45, scaled))
    }

    override fun stop() {
        ExtenderStatus.close()

        if (::future.isInitialized) future.cancel(true)
        screen.clear()
        screen.close()
    }
}
