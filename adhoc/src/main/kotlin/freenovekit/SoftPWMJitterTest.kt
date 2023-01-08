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

package freenovekit

import base.minutes
import com.diozero.api.ServoDevice
import com.diozero.api.ServoTrim
import com.diozero.devices.PwmLed
import java.lang.Thread.sleep

/**
 * Does it jitter?
 */
fun main() {
    PwmLed(27).use { led ->
        ServoDevice.Builder(17).setTrim(ServoTrim.TOWERPRO_SG90).build().apply {
            2 minutes {
                (0..180 step 5).forEach {
                    angle = it.toFloat()
                    led.value = it / 180f
                    sleep(1000)
                }
                value = -1f
                led.value = 0f
                sleep(1000)
                value = -0.5f
                led.value = .25f
                sleep(1000)
                value = 0f
                led.value = .5f
                sleep(1000)
                value = 0.5f
                led.value = .75f
                sleep(1000)
                value = 1f
                led.value = 1f
                sleep(1000)
            }
            value = 0f
        }.close()
    }
}
