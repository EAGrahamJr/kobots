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

import crackers.kobots.devices.display.SSD1327
import crackers.kobots.devices.qwiicKill
import crackers.kobots.devices.sensors.VCNL4040
import crackers.kobots.utilities.PointerGauge
import kobots.ops.createEventBus
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Try to portray stuff using a graphical "gauge"
 */
object GaugeStuff {
    private val running = AtomicBoolean(true)

    private val sensor by lazy {
        VCNL4040().apply {
            ambientLightEnabled = true
        }
    }

    private val oled by lazy { SSD1327(SSD1327.ADAFRUIT_STEMMA) }

    init {
        qwiicKill.whenPressed {
            println("BUTTON!")
            running.set(false)
        }

        val gauge: PointerGauge
        val image = BufferedImage(128, 128, BufferedImage.TYPE_BYTE_GRAY).apply {
            gauge = PointerGauge(
                graphics as Graphics2D,
                128,
                128,
                label = "LUM",
                fontColor = Color.GREEN,
                font = Font.SERIF
            )
        }

        createEventBus<Float>().also { bus ->
            bus.registerConsumer {
                gauge.setValue(it.toDouble())
                gauge.paint()
                oled.display(image)
                oled.show()
            }
            bus.registerPublisher { sensor.luminosity }
        }
    }

    fun run() {
        while (running.get()) {
            sleep(1000)
        }
        println("Done")
        sensor.close()
        oled.close()
        qwiicKill.close()
    }
}
