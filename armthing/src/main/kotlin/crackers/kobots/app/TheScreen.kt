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
import crackers.kobots.utilities.elapsed
import crackers.kobots.utilities.loadImage
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.Instant

/**
 * Return of the OLED!
 */
object TheScreen : AutoCloseable {
    private val screenGraphics: Graphics2D
    private val screen = SSD1327(SSD1327.ADAFRUIT_STEMMA).apply {
        clear()
    }
    private val image = BufferedImage(128, 128, BufferedImage.TYPE_BYTE_GRAY).also {
        screenGraphics = it.graphics as Graphics2D
    }
    private val sleeping by lazy { loadImage("/snooze.jpg") }
    private val okImage by lazy { loadImage("/smiley.png") }
    private val scanImage by lazy { loadImage("/not-smiley.jpg") }
    private val triggeredImage by lazy { loadImage("/screaming.png") }

    //    private var sleepTimer: Instant? = null
    private var lastImage: BufferedImage? = null
    private var sleepTimer: Instant? = null
    private val SLEEP_TIME = Duration.ofMinutes(2)

    fun start() {
        checkRun(10) { execute() }
    }

    private fun execute() {
        var nextImage = when (TheArm.state) {
            TheArm.State.BUSY -> {
                okImage
            }

            TheArm.State.REST -> {
                if (sleepTimer == null) sleepTimer = Instant.now()
                sleeping
            }

            TheArm.State.FRONT -> scanImage
            TheArm.State.GUARDING -> TODO()
            TheArm.State.CALIBRATION -> TODO()
        }
        // proximity alert over-rides image
        if (ControlThing.tooClose) nextImage = triggeredImage

        if (nextImage != lastImage) {
            sleepTimer = null
            lastImage = nextImage
            try {
                screen.displayOn = true
                screenGraphics.drawImage(nextImage, 0, 0, 128, 128, null)
                screen.display(image)
                screen.show()
            } catch (e: Exception) {
                println(e.localizedMessage)
                lastImage = null
            }
        } else if (sleepTimer != null) {
            if (sleepTimer!!.elapsed() > SLEEP_TIME) {
                screen.displayOn = false
                sleepTimer = null
            }
        }
    }

    override fun close() {
        screen.close()
    }

}
