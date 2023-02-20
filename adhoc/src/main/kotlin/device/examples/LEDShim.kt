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

package device.examples

import crackers.kobots.devices.at
import crackers.kobots.devices.lighting.PimoroniLEDShim
import crackers.kobots.utilities.colorInterval
import crackers.kobots.utilities.scale
import kobots.ops.createEventBus
import kobots.ops.registerCPUTempConsumer
import kobots.ops.registerConsumer
import kobots.ops.registerPublisher
import java.awt.Color
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

class LEDShim() : RunManager() {
    private val shim = PimoroniLEDShim()

    override fun close() {
        super.close()
        shim.close()
    }

    // mame use lf "last" values to avoid lots of unnecessary updates
    init {
        with(shim) {
            // CPU temperature in the upper 10
            val cpuColors = colorInterval(Color.RED, Color.GREEN, 10).map { it.scale(5) }.reversed()
            var lastTemp = -1
            registerCPUTempConsumer { temp ->
                println("Temp $temp")
                val x = ((temp - 40) * 10 / 30).roundToInt() + 18
                if (x != lastTemp) {
                    cpuColors.forEachIndexed { index, color -> pixelColor(index + 18, color) }
                    pixelColor(x, Color.WHITE)
                }
                lastTemp = x
            }

            // show x-axis in the lower 15
            createEventBus<Float>().also { bus ->
                val joyStickColors = (
                    colorInterval(Color.GREEN, Color.BLUE, 7).reversed() +
                        listOf(Color.GREEN) +
                        colorInterval(Color.GREEN, Color.BLUE, 7)
                    ).map { it.scale(5) }

                var lastX = -1
                bus.registerConsumer { x ->
                    val w = (14.0 * x).roundToInt()
                    if (w != lastX) {
                        joyStickColors.forEachIndexed { index, color -> pixelColor(index, color) }
                        pixelColor(w, Color.YELLOW)
                    }
                    lastX = w
                }
                val xAxis = crickit.signalAnalogIn(6)
                bus.registerPublisher { xAxis.unscaledValue }
            }

            // show y-axis on pixel 16 and servo
            createEventBus<Float>().also { bus ->
                var lastC = Color.BLUE
                bus.registerConsumer { y ->
                    val c = when {
                        y < 0.4 -> Color.RED
                        y > 0.6 -> Color.GREEN
                        else -> Color.BLACK
                    }
                    if (c != lastC) pixelColor(16, c)
                    lastC = c
                }
                val servo = crickit.servo(2)
                bus.registerConsumer { y ->
                    servo at 180 * y
                }
                val yAxis = crickit.signalAnalogIn(7)
                bus.registerPublisher { yAxis.unscaledValue }
            }
        }
    }
}

fun main() {
    LEDShim().use {
        it.waitForIt(.5.seconds)
    }
}
