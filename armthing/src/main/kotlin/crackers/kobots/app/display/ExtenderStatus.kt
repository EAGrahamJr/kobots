/*
 * Copyright 2022-2024 by E. A. Graham, Jr.
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

package crackers.kobots.app.display

import com.diozero.devices.oled.MonochromeSsdOled
import com.diozero.devices.oled.SH1106
import com.diozero.devices.oled.SSD1306
import com.diozero.devices.oled.SsdOledCommunicationChannel
import crackers.kobots.app.multiplexor
import crackers.kobots.graphics.toRadians
import crackers.kobots.graphics.widgets.VerticalPercentageIndicator
import java.awt.Font
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage

/**
 * Displays the extender status (0-100 percent) on a rotated 132x32 display.
 */
object ExtenderStatus {
    private val DISPLAY_HEIGHT = MonochromeSsdOled.Height.SHORT.lines

    private val screen: SSD1306 by lazy {
        val i2CDevice = multiplexor.getI2CDevice(2, SH1106.DEFAULT_I2C_ADDRESS)
        val channel = SsdOledCommunicationChannel.I2cCommunicationChannel(i2CDevice)
        SSD1306(channel, MonochromeSsdOled.Height.SHORT).apply { setContrast(0f) }
    }
    private var ready = false
    private var screenOn = false

    // set up the vertical thingie and then rotate it to display
    val rotatedGraphics: Graphics2D
    val rotatedImage =
        BufferedImage(MonochromeSsdOled.DEFAULT_WIDTH, DISPLAY_HEIGHT, BufferedImage.TYPE_BYTE_BINARY).also { img ->
            val theRotation = AffineTransform().apply {
                rotate(-90.toRadians())
                translate(-DISPLAY_HEIGHT.toDouble(), 0.0)
            }
            rotatedGraphics = (img.graphics as Graphics2D).apply { transform(theRotation) }
        }
    val vpd = VerticalPercentageIndicator(
        rotatedGraphics,
        Font(Font.SANS_SERIF, Font.BOLD, 10),
        MonochromeSsdOled.DEFAULT_WIDTH,
        label = "Lift"
    )

    fun update(percent: Int) {
        if (!ready) {
            vpd.drawStatic()
            ready = true
        }
        if (!screenOn) {
            screen.setDisplayOn(true)
            screenOn = true
        }
        vpd.updateValue(percent)
        screen.display(rotatedImage)
    }

    fun sleep() {
        screenOn = false
        screen.setDisplayOn(false)
        screen.clear()
    }

    fun close() {
        sleep()
        screen.close()
    }
}
