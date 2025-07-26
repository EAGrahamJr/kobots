/*
 * Copyright 2022-2025 by E. A. Graham, Jr.
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

package crackers.kobots.app.mechanicals

import com.diozero.api.I2CDevice
import com.diozero.devices.PCA9685
import com.diozero.devices.oled.SH1106
import crackers.kobots.devices.io.GamepadQT
import crackers.kobots.devices.io.QwiicTwist

/**
 * Masks whatever the bleep I'm doing with I2C
 */
object I2CFactory {
    val suziDevice by lazy {
        I2CDevice(1, PCA9685.DEFAULT_ADDRESS)
    }
    val armMonitorDevice by lazy {
        I2CDevice(1, SH1106.DEFAULT_I2C_ADDRESS)
    }
    val twistDevice by lazy {
        QwiicTwist.DEFAULT_I2C_DEVICE
    }
    val gamepadDevice by lazy {
        I2CDevice(1, GamepadQT.DEFAULT_I2C_ADDRESS)
    }
}
