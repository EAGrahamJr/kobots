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

package device.examples.adafruit

import base.REMOTE_PI
import com.diozero.api.ServoTrim
import crackers.kobots.devices.at
import crackers.kobots.devices.expander.CRICKITHatDeviceFactory
import crackers.kobots.devices.set
import crackers.kobots.ops.createEventBus
import crackers.kobots.ops.registerCPUTempConsumer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Use flows to take values from the joystick to move the servo using the CRICKIT board.
 */
fun main() {
    System.setProperty(REMOTE_PI, "marvin.local")

    val zButtonBus = createEventBus<Boolean>()
    val xAsisBus = createEventBus<Float>()
    val yAxisBus = createEventBus<Float>()
    val running = AtomicBoolean(true)

    CRICKITHatDeviceFactory().use {
        val servo = it.servo(2, ServoTrim.TOWERPRO_SG90)
        val xAxis = it.signalAnalogIn(6)
        val yAxis = it.signalAnalogIn(7)
        val zButton = it.signalDigitalIn(8)
        val noodle = it.signalDigitalOut(1)

        zButtonBus.registerConsumer {
            if (it) running.set(false)
        }
        xAsisBus.registerConsumer {
            servo at 180 * it
        }
        registerCPUTempConsumer {
            println("Temp $it")
        }
        yAxisBus.registerConsumer {
            noodle set (it > .5f)
        }

        xAsisBus.registerPublisher { xAxis.unscaledValue }
        yAxisBus.registerPublisher { yAxis.unscaledValue }
        zButtonBus.registerPublisher { zButton.value }

        runBlocking {
            while (running.get()) yield()
            noodle set false
        }
    }
}
