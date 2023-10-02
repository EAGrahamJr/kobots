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
import crackers.kobots.parts.app.KobotsSubscriber
import crackers.kobots.parts.app.io.StatusColumnDelegate
import crackers.kobots.parts.app.io.StatusColumnDisplay
import crackers.kobots.parts.app.joinTopic
import crackers.kobots.parts.loadImage
import crackers.kobots.parts.movement.SequenceExecutor
import crackers.kobots.parts.movement.SequenceExecutor.Companion.INTERNAL_TOPIC
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference

private const val MAX_WD = 128
private const val MAX_HT = 32

/**
 * Shows where the arm is on a timed basis.
 */
object ArmMonitor : StatusColumnDisplay by StatusColumnDelegate(MAX_WD, MAX_HT) {
    private val screenGraphics: Graphics2D

    private val monitorFont = Font(Font.SANS_SERIF, Font.PLAIN, 12)
    private val monitorMetrics: FontMetrics
    private var monitorLineHeight: Int

    private val monitorImage = BufferedImage(MAX_WD, MAX_HT, BufferedImage.TYPE_BYTE_GRAY).also { img: BufferedImage ->
        screenGraphics = (img.graphics as Graphics2D).also {
            monitorMetrics = it.getFontMetrics(monitorFont)
            monitorLineHeight = monitorMetrics.ascent + 1
        }
    }

    private val randomImages: List<BufferedImage>
    private val successImage: BufferedImage
    private val screen = run {
        val i2CDevice =
            I2CDevice(1, SSD1306.DEFAULT_I2C_ADDRESS)
//            multiplexer.channels[0]
        SSD1306(I2cCommunicationChannel(i2CDevice), SSD1306.Height.SHORT).apply {
            setContrast(0x20.toByte())
        }
    }

    private val lastStateReceived = AtomicReference<ArmState>()
    private lateinit var future: Future<*>

    init {
        randomImages = with(screen) {
            listOf(
                scaleImage(loadImage("/kissme.png")),
                scaleImage(loadImage("/meh.png")),
                scaleImage(loadImage("/oh-yeah.png")).also { successImage = it },
                scaleImage(loadImage("/stuck-out-tongue.png"))
            )
        }
    }

    fun start() {
        joinTopic(
            TheArm.STATE_TOPIC,
            KobotsSubscriber { message -> if (message is ArmState) lastStateReceived.set(message) }
        )
        joinTopic(
            SLEEP_TOPIC,
            KobotsSubscriber { event -> if (event is AppCommon.SleepEvent) screen.setDisplayOn(!event.sleep) }
        )
        joinTopic(
            INTERNAL_TOPIC,
            KobotsSubscriber { message ->
                if (message is SequenceExecutor.SequenceCompleted) {
                    drawAnImage(successImage)
                }
            }
        )

        val runner = {
            // if the arm is busy, show its status
            val lastState = lastStateReceived.get()
            if (lastState != null && lastState.busy || manualMode) showLastStatus()
            else showRandomImage()
        }

        future = AppCommon.executor.scheduleAtFixedRate(runner, 10, 10, java.util.concurrent.TimeUnit.MILLISECONDS)
    }

    private fun showLastStatus() = with(screenGraphics) {
        // show last recorded status
        lastStateReceived.get()?.let { state ->
            val mapped: Map<String, Number> = mapOf(
                "WST" to state.position.waist.angle,
                "XTN" to state.position.extender.angle,
                "ELB" to state.position.elbow.angle,
                "GRP" to state.position.gripper.angle
            )
            font = monitorFont
            displayStatuses(mapped)
        }
        screen.display(monitorImage)
    }

    // show a random image
    private var lastChanged = Instant.EPOCH
    private val CHANGE_IMAGE = Duration.ofSeconds(60)
    private val logger = LoggerFactory.getLogger("ArmMonitor")
    private fun showRandomImage() {
        if (Duration.between(lastChanged, Instant.now()) < CHANGE_IMAGE) return
        drawAnImage(randomImages.random())
        lastChanged = Instant.now()
    }

    // copy to the graphics 2D in about the center
    private fun drawAnImage(image: BufferedImage) {
        with(screenGraphics) {
            val ogColor = color
            color = Color.BLACK
            fillRect(0, 0, MAX_WD, MAX_HT)
            color = ogColor
            drawImage(image, (MAX_WD - image.width) / 2, (MAX_HT - image.height) / 2, null)
        }
        screen.display(monitorImage)
    }

    fun stop() {
        if (::future.isInitialized) future.cancel(true)
        screen.close()
    }
}
