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

package device.examples.qwiic.oled

import com.diozero.util.SleepUtil
import crackers.kobots.devices.display.SSD1327
import crackers.kobots.devices.qwiicKill
import crackers.kobots.devices.sensors.VCNL4040
import kobots.ops.registerConsumer
import kobots.ops.registerPublisher
import kobots.ops.theFlow
import kotlinx.coroutines.runBlocking
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_BYTE_GRAY
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

fun main() {
    System.setProperty("diozero.remote.hostname", "useless.local")
    val running = AtomicBoolean(true)

    qwiicKill.whenPressed {
        running.set(false)
        println("BUTTON!")
    }

    val oled = SSD1327(SSD1327.ADAFRUIT_STEMMA)
    val sensor = VCNL4040().apply {
        proximityEnabled = true
        ambientLightEnabled = true
        whiteLightEnabled = true
    }

    val image = BufferedImage(128, 128, TYPE_BYTE_GRAY)
    val graphics = (image.graphics as Graphics2D)
    val offset = graphics.let {
        it.font = Font(Font.SANS_SERIF, Font.PLAIN, 10)
        it.color = Color.WHITE
        it.drawString("PRX AMB WHL", 0, it.fontMetrics.ascent)
        it.fontMetrics.height + 3
    }

    theFlow.registerPublisher {
        Triple(sensor.proximity, sensor.luminosity, sensor.whiteLight)
    }

    // draw a graph for the 3 values - could use some better scaling here
    theFlow.registerConsumer {
        if (it is Triple<*, *, *>) {
            graphics.clearRect(0, offset, 127, 127)

            val maxHeight = 128 - offset

            // draw the proximity state (scale by degree)
            val proxValue = when {
                (it.first as Short) < 5 -> 0
                (it.first as Short) < 11 -> 1
                (it.first as Short) < 20 -> 3
                (it.first as Short) < 60 -> 6
                else -> 10
            } * 10
            graphics.color = Color.GREEN
            graphics.fillRect(0, 128 - (maxHeight * proxValue) / 100, 20, 127)

            graphics.color = Color.RED
            val lum = Math.min(it.second as Float, 100f).toInt()
            graphics.fillRect(23, 128 - (maxHeight * lum) / 100, 20, 127)

            graphics.color = Color.YELLOW
            val whl = Math.min((it.third as Short) / 2f, 100f).toInt()
            graphics.fillRect(47, 128 - (maxHeight * whl) / 100, 20, 127)

            oled.display(image)
            oled.show()
        }
    }

    runBlocking {
        while (running.get()) SleepUtil.busySleep(Duration.ofSeconds(5).toNanos())
        oled.close()
        qwiicKill.close()
    }
    exitProcess(0)
}
