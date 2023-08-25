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
import crackers.kobots.app.*
import crackers.kobots.app.io.NeoKeyMenu
import crackers.kobots.execution.KobotsSubscriber
import crackers.kobots.execution.executeWithMinTime
import crackers.kobots.execution.joinTopic
import crackers.kobots.utilities.center
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.time.Duration
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Shows where the arm is on a timed basis.
 */
object ArmMonitor : NeoKeyMenu.MenuDisplay {
    private lateinit var lastMenu: List<NeoKeyMenu.MenuItem>
    private const val MAX_WD = 128
    private const val MAX_HT = 32
    private const val HALF_HT = MAX_HT / 2
    private const val COL_WD = 30

    private val screenGraphics: Graphics2D

    private val monitorFont = Font(Font.SANS_SERIF, Font.PLAIN, 12)
    private val monitorMetrics: FontMetrics
    private var monitorLineHeight: Int

    private val busyFont = Font(Font.MONOSPACED, Font.BOLD, 8)
    private val busyTextLocation: Pair<Int, Int>

    private val image = BufferedImage(MAX_WD, MAX_HT, BufferedImage.TYPE_BYTE_GRAY).also { img: BufferedImage ->
        screenGraphics = (img.graphics as Graphics2D).also {
            monitorMetrics = it.getFontMetrics(monitorFont)
            monitorLineHeight = monitorMetrics.ascent + 1
            it.getFontMetrics(busyFont).run {
                busyTextLocation = (126 - stringWidth("B")) to ascent
            }
        }
    }

    private val screen by lazy {
        val i2CDevice = I2CDevice(1, SSD1306.DEFAULT_I2C_ADDRESS)
        SSD1306(I2cCommunicationChannel(i2CDevice), SSD1306.Height.SHORT)
    }

    private val lastStateReceived = AtomicReference<ArmState>()
    private val imageChanged = AtomicBoolean(false)
    private lateinit var future: Future<*>
    private val headers = listOf("WST", "XTN", "ELB", "GRP")

    fun start() {
        joinTopic(
            TheArm.STATE_TOPIC,
            KobotsSubscriber { message -> if (message is ArmState) lastStateReceived.set(message) }
        )
        joinTopic(SLEEP_TOPIC, KobotsSubscriber { event -> if (event is SleepEvent) screen.setDisplayOn(!event.sleep) })

        future = executor.submit {
            while (runFlag.get()) {
                executeWithMinTime(Duration.ofMillis(10)) {
                    // if the arm is busy, show its status
                    val lastState = lastStateReceived.get()
                    val showStatus = lastState != null && lastState.busy || manualMode
                    if (showStatus) showLastStatus()
                    else {
                        if (lastState != null) {
                            lastStateReceived.set(null)
                            display(lastMenu.toList())
                        }
                    }
                    if (imageChanged.getAndSet(false)) screen.display(image)
                }
            }
        }
    }

    private fun Graphics2D.clearImage() {
        color = Color.BLACK
        fillRect(0, 0, MAX_WD, MAX_HT)
    }

    private fun showLastStatus() = with(screenGraphics) {
        // show last recorded status
        lastStateReceived.get()?.let { state ->

            clearImage()

            font = monitorFont
            // position headers
            for (i in 0 until 4) {
                color = Color.GRAY
                fillRect(i * COL_WD, 0, COL_WD - 1, HALF_HT)
                color = Color.BLACK
                val x = monitorMetrics.center(headers[i], COL_WD - 1)
                drawString(headers[i], (COL_WD * i) + x, monitorMetrics.ascent)
            }

            state.position.let { arm ->
                listOf(arm.waist, arm.extender, arm.elbow, arm.gripper)
            }.map {
                it.angle.toString()
            }.forEachIndexed { i, string ->
                color = Color.WHITE
                val x = monitorMetrics.center(string, COL_WD - 1)
                drawString(string, (COL_WD * i) + x, MAX_HT - 1)
            }
        }
        imageChanged.set(true)
    }

    fun stop() {
        future.get()
        screen.close()
    }

    override fun display(items: List<NeoKeyMenu.MenuItem>) = with(screenGraphics) {
        lastMenu = items
        clearImage()
        color = Color.WHITE
        font = monitorFont

        drawString(items[0].toString(), 0, monitorLineHeight)
        color = Color.BLACK
        fillRect(MAX_WD / 2, 0, MAX_WD, monitorLineHeight)
        color = Color.WHITE
        drawString("  " + items[1].toString(), (MAX_WD / 2) - 3, monitorLineHeight)

        drawString(items[2].toString(), 0, MAX_HT)
        color = Color.BLACK
        fillRect(MAX_WD / 2, HALF_HT, MAX_WD, MAX_HT)
        color = Color.WHITE
        val s = "  ${items[3]}  "
        val xn = MAX_WD - monitorMetrics.stringWidth(s)
        drawString(s, xn, MAX_HT)

        imageChanged.set(true)
    }
}
