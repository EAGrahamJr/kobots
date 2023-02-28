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
package crackers.kobots.devices.display

import com.diozero.devices.HD44780Lcd
import com.diozero.devices.LcdConnection

/**
 * Pre-defined LCD objects. Closes the back-pack when the display is closed.
 */
object HD44780Lcd {
    /**
     * Backpack
     */
    val PiBackpack by lazy { LcdConnection.PCF8574LcdConnection(1) }

    /**
     * Default 2-line display
     */
    val Pi2Line by lazy {
        object : HD44780Lcd(PiBackpack, 16, 2) {
            override fun close() {
                super.close()
                PiBackpack.close()
            }
        }
    }

    /**
     * Default 4-line display
     */
    val Pi4Line by lazy {
        object : HD44780Lcd(PiBackpack, 20, 4) {
            override fun close() {
                super.close()
                PiBackpack.close()
            }
        }
    }
}
