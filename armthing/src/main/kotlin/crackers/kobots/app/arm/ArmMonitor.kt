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

package crackers.kobots.app.arm

import com.diozero.api.I2CDevice
import com.diozero.devices.oled.SH1106
import com.diozero.devices.oled.SSD1306
import com.diozero.devices.oled.SsdOledCommunicationChannel.I2cCommunicationChannel
import crackers.kobots.app.AppCommon
import crackers.kobots.app.AppCommon.whileRunning
import crackers.kobots.app.enviro.DieAufseherin
import crackers.kobots.parts.app.KobotSleep
import crackers.kobots.parts.app.io.StatusColumnDelegate
import crackers.kobots.parts.app.io.StatusColumnDisplay
import crackers.kobots.parts.app.io.graphics.*
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
object ArmMonitor : StatusColumnDisplay by StatusColumnDelegate(MAX_WD, MAX_HT), KobotRadar by SimpleRadar(
    Point(0, 0),
    MAX_HT.toDouble()
) {
    private val logger = LoggerFactory.getLogger("ArmMonitor")
    private val TURN_OFF = Duration.ofMinutes(5)

    private val imageType = BufferedImage.TYPE_BYTE_BINARY

    private val eyeGraphics: Graphics2D
    private val eyeImage = BufferedImage(MAX_WD, MAX_HT, imageType).also { img: BufferedImage ->
        eyeGraphics = (img.graphics as Graphics2D)
    }

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
        val i2CDevice = I2CDevice(1, SSD1306.DEFAULT_I2C_ADDRESS)
        SH1106(I2cCommunicationChannel(i2CDevice)).apply { setContrast(0x20.toByte()) }
    }


    private lateinit var future: Future<*>

    private var screenOnAt = Instant.EPOCH

    private val LOOK_RIGHT = Expression(
        lidPosition = Eye.LidPosition.ONE_QUARTER,
        pupilPosition = Pupil.Position.RIGHT + Pupil.Position.CENTER
    )
    private val LOOK_LEFT = Expression(
        lidPosition = Eye.LidPosition.ONE_QUARTER,
        pupilPosition = Pupil.Position.LEFT + Pupil.Position.CENTER
    )

    fun start() {
        screenOnAt = Instant.now()
        var screenOn = true
        future = AppCommon.executor.scheduleWithFixedDelay(10.milliseconds, 10.milliseconds) {

            whileRunning {
                var drawStats = false
                val screenDirty = when (DieAufseherin.currentMode) {
                    DieAufseherin.SystemMode.IDLE -> {
                        val random = (CannedExpressions.entries - CannedExpressions.CLOSED).random().expression
                        showEyes(random)
                    }

                    DieAufseherin.SystemMode.IN_MOTION -> TODO()
                    DieAufseherin.SystemMode.MANUAL -> {
                        // screen is always dirty amd enabled
                        screenOnAt = Instant.now()
                        statsGraphics.displayStatuses(ManualController.statuses())
                        showEyes(LOOK_LEFT)
                        drawStats = true
                        true
                    }

                    DieAufseherin.SystemMode.SHUTDOWN -> {
                        showEyes(CannedExpressions.CLOSED.expression) // hmmmm
                        false
                    }
                }

                if (screenDirty) {
                    if (!screenOn) {
                        screen.setDisplayOn(true)
                        screenOn = true
                    }

                    screenGraphics.clearRect(0, 0, MAX_WD, 2 * MAX_HT)
                    screenGraphics.drawImage(eyeImage, 0, 0, null)
                    if (drawStats) screenGraphics.drawImage(statsImage, 0, MAX_HT, null)
                    screen.display(screenImage)
                } else {
                    if (screenOnAt.elapsed() > TURN_OFF && screenOn) {
                        screen.setDisplayOn(false)
                        screenOn = false
                    }
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

    // show a random image
    private val GO_TO_SLEEP = TURN_OFF.minusMinutes(2)
    private val CHANGE_EYES = Duration.ofSeconds(10)
    private var eyesLastChanged = Instant.EPOCH
    private var eyesClosed = false

    private val leftEye = Eye(Point(46, 15), 15)
    private val rightEye = Eye(Point(82, 15), 15)
    private val eyes = PairOfEyes(leftEye, rightEye)

    /**
     * Show the eyes: they are closed at first and then "opened" to the desired expression; unless time has elapsed for
     * showing anything, leave the eyes closed.
     */
    @Synchronized
    private fun showEyes(expression: Expression): Boolean {
        // time to go to sleep or have we been sleeping?
        if (screenOnAt.elapsed() >= GO_TO_SLEEP) {
            if (eyesClosed) return false
            closeEyes()
            return true
        }

        // are we actually going to change expression?
        if (eyesLastChanged.elapsed() < CHANGE_EYES) return false

        // do a blink
        closeEyes()
        eyesClosed = false
        KobotSleep.millis(200)
        eyes(expression)
        drawEyes()
        eyesLastChanged = Instant.now()
        return true
    }

    private fun closeEyes() {
        eyeGraphics.color = Color.BLACK
        eyeGraphics.fillRect(0, 0, MAX_WD, MAX_HT)
        eyes(CannedExpressions.CLOSED.expression)
        drawEyes()
        eyesClosed = true
    }


    private fun drawEyes() {
        eyes.draw(eyeGraphics)
        screen.display(screenImage)
    }

    fun stop() {
        if (::future.isInitialized) future.cancel(true)
        screen.close()
    }
}
