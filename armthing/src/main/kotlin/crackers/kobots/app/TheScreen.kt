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

package crackers.kobots.app

import crackers.kobots.devices.display.SSD1327
import crackers.kobots.utilities.loadImage
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.lang.Thread.sleep

/**
 * Return of the OLED!
 */
object TheScreen : AutoCloseable {

    val screenGraphics: Graphics2D
    val screen = SSD1327(SSD1327.ADAFRUIT_STEMMA).apply {
        clear()
    }
    val image = BufferedImage(128, 128, BufferedImage.TYPE_BYTE_GRAY).also {
        screenGraphics = it.graphics as Graphics2D
    }
    val sleeping by lazy { loadImage("/snooze.jpg") }
    val okImage by lazy { loadImage("/smiley.png") }
    val scanImage by lazy { loadImage("/not-smiley.jpg") }

    private var lastImage: BufferedImage? = null
    fun execute() {
        val nextImage = when (TheArm.state) {
            TheArm.State.BUSY -> okImage
            TheArm.State.REST -> sleeping
            TheArm.State.FRONT -> scanImage
            TheArm.State.GUARDING -> TODO()
        }
        if (nextImage != lastImage) {
            screen.displayOn = true
            screenGraphics.drawImage(nextImage, 0, 0, 128, 128, null)
            lastImage = nextImage
            screen.display(image)
            screen.show()
            if (TheArm.state == TheArm.State.REST) {
                sleep(100)
                screen.displayOn = false
            }
        }
    }

    override fun close() {
        screen.close()
    }
}