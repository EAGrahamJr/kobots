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

import com.diozero.api.I2CDevice
import com.diozero.devices.oled.SSD1306
import com.diozero.devices.oled.SsdOledCommunicationChannel
import crackers.kobots.StatusColumnDelegate
import crackers.kobots.StatusColumnDisplay
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage

/**
 * Shows where everything is
 */
object ServoDisplay : StatusColumnDisplay by StatusColumnDelegate(128, 32) {
    private val screen by lazy {
        val i2CDevice = I2CDevice(1, SSD1306.DEFAULT_I2C_ADDRESS)
        SSD1306(SsdOledCommunicationChannel.I2cCommunicationChannel(i2CDevice), SSD1306.Height.SHORT)
    }
    private val screenGraphics: Graphics2D
    private val image = BufferedImage(128, 32, BufferedImage.TYPE_BYTE_GRAY).also { img: BufferedImage ->
        val monitorFont = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        screenGraphics = (img.graphics as Graphics2D).apply {
            font = monitorFont
        }
    }

    fun sleep(b: Boolean) {
        screen.setDisplayOn(!b)
    }

    fun show(rotoLocation: Int, liftLocation: Int) {
        screenGraphics.displayStatuses(mapOf("Roto" to rotoLocation, "Lift" to liftLocation))
        screen.display(image)
    }
}
