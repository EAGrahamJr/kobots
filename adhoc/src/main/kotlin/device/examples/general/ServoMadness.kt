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

package device.examples.general

import base.MARVIN
import base.REMOTE_PI
import com.diozero.util.SleepUtil
import crackers.kobots.devices.at
import crackers.kobots.devices.expander.CRICKITHatDeviceFactory
import java.time.Duration

/**
 * Whee!!!
 */
class ServoMadness {
    val factory by lazy {
        CRICKITHatDeviceFactory()
    }

    // joystick
    val zButton by lazy { factory.signalDigitalIn(8) }
    val yAxis by lazy { factory.signalAnalogIn(7) }
    val xAxis by lazy { factory.signalAnalogIn(6) }

    val servo1 by lazy { factory.servo(1).apply { angle = 90f } }
    val servo2 by lazy { factory.servo(2).apply { angle = 90f } }

    fun execute() {
        var lastX = 0
        var lastY = 0

        factory.use {
            while (!zButton.value) {
                xAxis.unscaledValue.also { x ->
                    val fixed = (x * 100).toInt()
                    if (fixed != lastX) {
                        lastX = fixed
                        servo2 at 180 * fixed / 100f
                    }
                }
                yAxis.unscaledValue.also { y ->
                    val fixed = (y * 100).toInt()
                    if (fixed != lastY) {
                        lastY = fixed
                        servo1 at 180 * fixed / 100f
                    }
                }
                SleepUtil.busySleep(Duration.ofMillis(50).toNanos())
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty(REMOTE_PI, MARVIN)
            ServoMadness().execute()
        }
    }
}
