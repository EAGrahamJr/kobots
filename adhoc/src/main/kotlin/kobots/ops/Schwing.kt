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

package kobots.ops

import com.diozero.api.ServoTrim
import crackers.kobots.devices.at
import crackers.kobots.devices.expander.CRICKITHatDeviceFactory
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

val zFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST)
val xFlow = MutableSharedFlow<Float>(extraBufferCapacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST)

private val running = AtomicBoolean(true)

/**
 * Use flows to take values from the joystick to move the servo using the CRICKIT board
 */
fun main() {
    System.setProperty("diozero.remote.hostname", "marvin.local")

    CRICKITHatDeviceFactory().use {
        val servo = it.servo(4, ServoTrim.TOWERPRO_SG90)
        val xAxis = it.signalAnalogIn(6)
        val yAxis = it.signalAnalogIn(7)
        val zButton = it.signalDigitalIn(8)

        registerConsumer(zFlow) { value ->
            if (value) running.set(false)
        }
        registerConsumer(xFlow) { value ->
            servo at 180 * value
        }
        registerConsumer(theFlow) {
            if (it is Double) println("Temp $it")
        }

        registerPublisher(xFlow) { xAxis.unscaledValue }
        registerPublisher(zFlow) { zButton.value }
        registerPublisher(theFlow, Duration.ofSeconds(1)) { cpuTemp() }

        runBlocking { while (running.get()) yield() }
    }
}

private fun cpuTemp() = Files.readAllLines(Paths.get("/sys/class/thermal/thermal_zone0/temp")).first().toDouble() / 1000
