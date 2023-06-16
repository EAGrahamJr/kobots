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
import crackers.kobots.app.KobotsSubscriber
import crackers.kobots.app.executor
import crackers.kobots.app.joinTopic
import crackers.kobots.app.runFlag
import crackers.kobots.utilities.KobotSleep
import crackers.kobots.utilities.center
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Shows where the arm is on a timed basis.
 */
object ArmMonitor {
    private const val MW = 128
    private const val MH = 32
    private const val HALF_HT = MH / 2
    private const val COL_WD = 30

    private val screenGraphics: Graphics2D

    private val monitorFont = Font(Font.SANS_SERIF, Font.PLAIN, 10)
    private val monitorMetrics: FontMetrics
    private var monitorLineHight: Int

    private val busyFont = Font(Font.MONOSPACED, Font.BOLD, 8)
    private val busyTextLocation: Pair<Int, Int>

    private val image = BufferedImage(MW, MH, BufferedImage.TYPE_BYTE_GRAY).also { img: BufferedImage ->
        screenGraphics = (img.graphics as Graphics2D).also {
            monitorMetrics = it.getFontMetrics(monitorFont)
            monitorLineHight = monitorMetrics.ascent + 1
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
    private val myRunFlag = AtomicBoolean(true)
    private lateinit var future: Future<*>

    fun start() {
        joinTopic(TheArm.STATE_TOPIC, KobotsSubscriber { message ->
            if (message is ArmState) lastStateReceived.set(message)
        })
        future = executor.submit {
            // write column headers - each one gets 1/4 display (?)
            screen.clear()
            with(screenGraphics) {

                val headers = listOf("WST", "SHL", "ELB", "GRP")
                for (i in 0 until 4) {
                    color = Color.GRAY
                    fillRect(i * COL_WD, 0, COL_WD - 1, HALF_HT)
                    color = Color.BLACK
                    val x = monitorMetrics.center(headers[i], COL_WD - 1)
                    drawString(headers[i], (COL_WD * i) + x, monitorMetrics.ascent)
                }
            }
            screen.display(image)
            while (runFlag.get() && myRunFlag.get()) {
                // show last recorded status
                lastStateReceived.get()?.let { state ->
                    val values = state.position.let { arm ->
                        listOf(arm.waist, arm.shoulder, arm.elbow, arm.gripper)
                    }.map {
                        it.angle.toInt().toString()
                    }
                    with(screenGraphics) {
                        for (i in 0 until 4) {
                            color = Color.BLACK
                            fillRect(i * COL_WD, HALF_HT, COL_WD - 1, HALF_HT)
                            color = Color.WHITE
                            val x = monitorMetrics.center(values[i], COL_WD - 1)
                            drawString(values[i], (COL_WD * i) + x, MH - 1)
                        }
//                        drawString(ProximitySensor.proximity.toString(), 120, 31)
                        if (state.busy) {
                            Color.RED
                            drawString("B", busyTextLocation.first, busyTextLocation.second)
                        } else {
                            color = Color.BLACK
                            fillRect(busyTextLocation.first, 0, 10, busyTextLocation.second)
                        }
                    }
                    screen.display(image)
                }
                KobotSleep.millis(100)
            }
        }
    }

    fun stop() {
        myRunFlag.set(false)
        future.get()
        screen.close()
    }
}
