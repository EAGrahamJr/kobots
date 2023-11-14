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
import crackers.kobots.app.AppCommon.whileRunning
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

    private val screenImage = BufferedImage(MAX_WD, MAX_HT, BufferedImage.TYPE_BYTE_GRAY).also { img: BufferedImage ->
        screenGraphics = (img.graphics as Graphics2D).apply {
            font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        }
    }

    private val screen by lazy {
        val i2CDevice = I2CDevice(1, SSD1306.DEFAULT_I2C_ADDRESS)
        SSD1306(I2cCommunicationChannel(i2CDevice), SSD1306.Height.SHORT).apply { setContrast(0x20.toByte()) }
    }

    private enum class Mode {
        MANUAL, IDLE, SCAN, SLEEP, MOVING, COMPLETED
    }

    private lateinit var future: Future<*>
    private val atomicMode = AtomicReference(Mode.IDLE)
    private var mode: Mode
        get() = atomicMode.get()
        set(value) = atomicMode.set(value)

    private var lastChanged = Instant.EPOCH
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
        joinTopic(SLEEP_TOPIC, { event: AppCommon.SleepEvent -> if (event.sleep) mode = Mode.SLEEP })
        joinTopic(INTERNAL_TOPIC, { message: SequenceExecutor.SequenceEvent ->
            mode = if (message.started) {
                if (message.sequence.lowercase().contains("scan")) Mode.SCAN
                else {
                    showEyes(CannedExpressions.LOOK_UP.expression, true)
                    Mode.MOVING
                }
            } else {
                Mode.COMPLETED
            }
        }
        )
        screenOnAt = Instant.now()

        future = AppCommon.executor.scheduleWithFixedDelay(10.milliseconds, 10.milliseconds) {
            var lastScanLocation = 0

            whileRunning {
                // manual mode, show where the arm is at
                if (manualMode) {
                    screen.setDisplayOn(true)
                    screenOnAt = Instant.now()
                    showLastStatus()
                    mode = Mode.MANUAL
                } else {
                    when (mode) {
                        // the showEyes function controls screen on/off and expression changes
                        Mode.IDLE -> {
                            val random = (CannedExpressions.entries - CannedExpressions.CLOSED).random().expression
                            showEyes(random)
                        }

                        Mode.SCAN -> {
                            screenOnAt = Instant.now()
                            // if the waist location is greater than the last location, look to the right otherwise left
                            val current = TheArm.state.position.waist.angle
                            if (current > lastScanLocation) showEyes(LOOK_RIGHT)
                            else showEyes(LOOK_LEFT)
                            lastScanLocation = current
                        }

                        Mode.MOVING -> {
                            // above
                        }

                        Mode.COMPLETED -> {
                            showEyes(Expression(lidPosition = Eye.LidPosition.ONE_QUARTER), true)
                            mode = Mode.IDLE
                        }

                        Mode.SLEEP -> {
                            showEyes(CannedExpressions.CLOSED.expression, true)
                            mode = Mode.IDLE
                        }

                        else -> mode = Mode.IDLE
                    }
                }
            }
        }
    }

    private fun showLastStatus() = with(screenGraphics) {
        // show last recorded status
        displayStatuses(TheArm.state.position.mapped())
        screen.display(screenImage)
    }

    // show a random image
    private val CHANGE_EYES = Duration.ofSeconds(10)
    private val TURN_OFF = Duration.ofMinutes(5)

    private val leftEye = Eye(Point(46, 15), 15)
    private val rightEye = Eye(Point(82, 15), 15)
    private val eyes = PairOfEyes(leftEye, rightEye)

    /**
     * Show the eyes -- if time has elapsed for showing anything, turn off the screen unless forced.
     */
    @Synchronized
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
            // time to change expression or not?
            if (lastChanged.elapsed() < CHANGE_EYES) return
        }

        if (force) screenOnAt = Instant.now()
        screen.setDisplayOn(true)

        screenGraphics.color = Color.BLACK
        screenGraphics.fillRect(0, 0, MAX_WD, MAX_HT)

        eyes(CannedExpressions.CLOSED.expression)
        drawEyes()
        KobotSleep.millis(200)

        eyes(expression)
        drawEyes()
        lastChanged = Instant.now()
    }


    private fun drawEyes() {
        eyes.draw(screenGraphics)
        screen.display(screenImage)
    }

    fun stop() {
        if (::future.isInitialized) future.cancel(true)
        screen.close()
    }
}
