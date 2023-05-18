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

package dork

import com.diozero.api.I2CDevice
import com.diozero.api.ServoDevice
import com.diozero.api.ServoTrim
import com.diozero.devices.oled.SSD1306
import com.diozero.devices.oled.SSD1306.DEFAULT_I2C_ADDRESS
import com.diozero.devices.oled.SsdOledCommunicationChannel.I2cCommunicationChannel
import com.diozero.sbc.LocalSystemInfo
import crackers.kobots.devices.expander.CRICKITHatDeviceFactory
import crackers.kobots.devices.sensors.VCNL4040
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.lang.Thread.sleep
import java.time.LocalTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private val danceFlag = AtomicBoolean(false)

private class ServoWrapper(val servo: ServoDevice) : Runnable {
    init {
        servo.angle = 0f
    }

    var direction = 1
    fun move() {
        val current = servo.angle.toInt()
        when (current) {
            180 -> direction = -1
            0 -> direction = 1
        }
        servo.angle = current.toFloat() + direction
    }

    override fun run() {
        while (danceFlag.get()) {
            move()
            sleep(13)
        }
        servo.angle = 0f
    }
}

private val executor = Executors.newScheduledThreadPool(3)

fun danseMacbre() {
//    System.setProperty(REMOTE_PI, MARVIN)
    CRICKITHatDeviceFactory().use { crickit ->
        val button = crickit.touchDigitalIn(4)
        val servos: List<ServoWrapper> = listOf(
            ServoWrapper(crickit.servo(4, ServoTrim.TOWERPRO_SG90)),
            ServoWrapper(crickit.servo(3, ServoTrim.TOWERPRO_SG90)),
            ServoWrapper(crickit.servo(2, ServoTrim.TOWERPRO_SG90))
        )

        VCNL4040().use { sensor ->
            sensor.ambientLightEnabled = true
            val channel = I2cCommunicationChannel(I2CDevice(1, DEFAULT_I2C_ADDRESS))
            SSD1306(channel, SSD1306.Height.SHORT).use { display ->
                val textImage = BufferedImage(128, 32, BufferedImage.TYPE_BYTE_BINARY)
                val timeFont = Font(Font.SANS_SERIF, Font.PLAIN, 10)
                val stuffFont = Font(Font.SANS_SERIF, Font.BOLD, 18)

                while (!button.value) {
                    val luminosity = sensor.luminosity
                    with(textImage.graphics as Graphics2D) {
                        color = Color.BLACK
                        fillRect(0, 0, 128, 32)
                        color = Color.WHITE
                        font = timeFont
                        val time = LocalTime.now().let { String.format("%2d:%02d:%02d", it.hour, it.minute, it.second) }
                        val timeHeight = fontMetrics.ascent
                        drawString(time, 0, timeHeight)
                        drawString("CPU: ${LocalSystemInfo.getInstance().cpuTemperature}", 64, timeHeight)
                        val stuff = "LUM: $luminosity"
                        font = stuffFont
                        val stuffHeight = fontMetrics.ascent
                        drawString(stuff, 0, timeHeight + stuffHeight)
                    }

                    display.display(textImage)
                    if (luminosity > 100) {
                        if (!danceFlag.get()) {
                            danceFlag.set(true)
                            executor.submit(servos[0])
                            executor.schedule(servos[1], 1, TimeUnit.SECONDS)
                            executor.schedule(servos[2], 2, TimeUnit.SECONDS)
                        }
                    } else {
                        danceFlag.compareAndSet(true, false)
                    }
                    sleep(100)
                }
                servos.forEach { it.servo.angle = 0f }
            }
        }
    }
}
