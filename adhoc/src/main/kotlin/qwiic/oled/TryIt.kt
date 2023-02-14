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

package qwiic.oled

import crackers.kobots.devices.display.GrayOled
import crackers.kobots.devices.display.SSD1327
import crackers.kobots.devices.display.SSD1327.Companion.ADAFRUIT_STEMMA
import java.awt.Color
import java.awt.Font
import java.awt.Font.*
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_BYTE_GRAY
import java.lang.Thread.sleep
import javax.imageio.ImageIO
import kotlin.system.exitProcess

/**
 * Figuring out graphics translations.
 */
fun main() {
    System.setProperty("diozero.remote.hostname", "useless.local")

    SSD1327(ADAFRUIT_STEMMA).use { oled ->
        oled.loadAndShow("/grayscale.png")
        sleep(5000)
        oled.loadAndShow("/adafruit-logo.png")
        sleep(5000)
        oled.loadAndShow("/standard-test-image-grayscale.png")
        sleep(5000)
        oled.loadAndShow("/color-test-image.jpg")
        sleep(5000)

        BufferedImage(128, 128, TYPE_BYTE_GRAY).apply {
            graphics.let { it: Graphics ->
                val g = it as Graphics2D
                sayHello(g)
            }
        }.let {
            oled.display(it)
            oled.show()
        }
        sleep(10000)
    }
    exitProcess(0)
}

fun GrayOled.loadAndShow(name: String) {
    display(loadImage(name))
    show()
}

fun sayHello(g: Graphics2D) {
    g.color = Color.WHITE
    g.font = Font(SANS_SERIF, PLAIN, 12)
    val yOffset = g.fontMetrics.ascent
    g.drawChars("Hello, World!!!".toCharArray(), 0, 15, 0, yOffset)
    g.color = Color.RED
    g.font = Font(SERIF, BOLD, 16)
    g.drawChars("Hello, Casey!".toCharArray(), 0, 13, 5, yOffset * 3)
    g.dispose()
}

fun loadImage(name: String) = ImageIO.read(object {}.javaClass.getResourceAsStream(name))
