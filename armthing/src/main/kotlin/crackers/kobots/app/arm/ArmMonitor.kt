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

package crackers.kobots.app.arm

import com.diozero.api.I2CDevice
import com.diozero.devices.oled.SSD1306
import com.diozero.devices.oled.SsdOledCommunicationChannel.I2cCommunicationChannel
import crackers.kobots.app.AppCommon
import crackers.kobots.app.AppCommon.SLEEP_TOPIC
import crackers.kobots.app.manualMode
import crackers.kobots.parts.app.KobotSleep
import crackers.kobots.parts.app.io.StatusColumnDelegate
import crackers.kobots.parts.app.io.StatusColumnDisplay
import crackers.kobots.parts.app.io.graphics.*
import crackers.kobots.parts.app.joinTopic
import crackers.kobots.parts.elapsed
import crackers.kobots.parts.movement.SequenceExecutor
import crackers.kobots.parts.movement.SequenceExecutor.Companion.INTERNAL_TOPIC
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.milliseconds

private const val MAX_WD = 128
private const val MAX_HT = 32

/**
 * Shows where the arm is on a timed basis.
 */
object ArmMonitor : StatusColumnDisplay by StatusColumnDelegate(MAX_WD, MAX_HT) {
    private val logger = LoggerFactory.getLogger("ArmMonitor")
    private val screenGraphics: Graphics2D

    private val monitorImage = BufferedImage(MAX_WD, MAX_HT, BufferedImage.TYPE_BYTE_GRAY).also { img: BufferedImage ->
        screenGraphics = (img.graphics as Graphics2D).apply {
            font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        }
    }

    private val screen = run {
        val i2CDevice = I2CDevice(1, SSD1306.DEFAULT_I2C_ADDRESS)
        SSD1306(I2cCommunicationChannel(i2CDevice), SSD1306.Height.SHORT).apply { setContrast(0x20.toByte()) }
    }

    private lateinit var future: Future<*>

    private val lastStateReceived = AtomicReference<ArmState>()
    private var lastChanged = Instant.EPOCH
    private var screenOnAt = Instant.EPOCH
    private val needsClear = AtomicBoolean(true)

    fun start() {
        joinTopic(TheArm.STATE_TOPIC, { message: ArmState -> lastStateReceived.set(message) })
        joinTopic(
            SLEEP_TOPIC,
            { event: AppCommon.SleepEvent -> if (event.sleep) showEyes(CannedExpressions.CLOSED.expression, true) }
        )
        joinTopic(INTERNAL_TOPIC, { message: SequenceExecutor.SequenceEvent ->
            if (message.started) {
                showEyes(CannedExpressions.WHATS_THIS.expression shift Pupil.Position.HALF_RIGHT, true)
                needsClear.set(true)
            } else {
                showEyes(Expression(lidPosition = Eye.LidPosition.ONE_QUARTER), true)
                needsClear.set(true)
            }
        }
        )
        screenOnAt = Instant.now()

        future = AppCommon.executor.scheduleWithFixedDelay(10.milliseconds, 10.milliseconds) {
            // if the arm is busy, show its status
            val lastState = lastStateReceived.get()
            if (lastState != null && lastState.busy || manualMode) {
                screen.setDisplayOn(true)
                screenOnAt = Instant.now()

                if (manualMode) showLastStatus()
            } else {
                val random = (CannedExpressions.entries - CannedExpressions.CLOSED).random().expression
                showEyes(random)
            }
        }
    }

    private fun showLastStatus() = with(screenGraphics) {
        // show last recorded status
        lastStateReceived.get()?.let { state ->
            displayStatuses(state.position.mapped())
        }
        screen.display(monitorImage)
        needsClear.set(true)
    }

    // show a random image
    private val CHANGE_EYES = Duration.ofSeconds(10)
    private val TURN_OFF = Duration.ofMinutes(5)

    private val leftEye = Eye(Point(46, 15), 15)
    private val rightEye = Eye(Point(82, 15), 15)
    private val eyes = PairOfEyes(leftEye, rightEye)

    fun showEyes(expression: Expression, force: Boolean = false) {
        if (!force) {
            screenOnAt.elapsed().also { elapsed ->
                if (elapsed > TURN_OFF) {
                    screen.setDisplayOn(false)
                    return
                }
                if (elapsed > TURN_OFF.minusMinutes(2)) {
                    eyes(CannedExpressions.CLOSED.expression)
                    drawEyes()
                    return
                }
            }
            if (lastChanged.elapsed() < CHANGE_EYES) return
        }
        if (force) screenOnAt = Instant.now()

        checkAndClear()

        eyes(CannedExpressions.CLOSED.expression)
        drawEyes()
        KobotSleep.millis(200)

        eyes(expression)
        drawEyes()
        lastChanged = Instant.now()
    }

    private fun checkAndClear() {
        if (needsClear.get()) {
            screenGraphics.color = Color.BLACK
            screenGraphics.fillRect(0, 0, MAX_WD, MAX_HT)
            needsClear.set(false)
        }
    }

    private fun drawEyes() {
        eyes.draw(screenGraphics)
        screen.display(monitorImage)
    }

    fun stop() {
        if (::future.isInitialized) future.cancel(true)
        screen.close()
    }
}
