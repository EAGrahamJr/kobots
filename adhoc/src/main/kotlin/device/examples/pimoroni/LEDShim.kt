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

package device.examples.pimoroni

import base.REMOTE_PI
import crackers.kobots.devices.at
import crackers.kobots.devices.lighting.PimoroniLEDShim
import crackers.kobots.utilities.colorInterval
import crackers.kobots.utilities.scale
import device.examples.RunManagerForFlows
import kobots.ops.createEventBus
import kobots.ops.registerCPUTempConsumer
import java.awt.Color
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

class LEDShim() : RunManagerForFlows() {
    private val shim = PimoroniLEDShim()

    override fun close() {
        super.close()
        shim.close()
    }

    // mame use lf "last" values to avoid lots of unnecessary updates
    init {
        with(shim) {
            // CPU temperature in the upper 10
            val OFFSET = 18
            val SIZE = 10
            val cpuColors = colorInterval(Color.RED, Color.GREEN, SIZE).map { it.scale(5) }.reversed().apply {
                forEachIndexed { index, color -> pixelColor(index + OFFSET, color) }
            }
            var lastTemp = -1
            registerCPUTempConsumer { temp ->
                println("Temp $temp")
                // 30 degree range, offset by 40
                val x = ((temp - 40) * SIZE / 30).roundToInt()
                if (x in (0..9) && x != lastTemp) {
                    if (lastTemp != -1) pixelColor(lastTemp + OFFSET, cpuColors[lastTemp])
                    pixelColor(x + OFFSET, Color.WHITE)
                    lastTemp = x
                }
            }

            // show x-axis in the lower 15
            createEventBus<Float>().also { bus ->
                val g2b = colorInterval(Color.GREEN, Color.BLUE, 7)
                // blue on outside to greed on inside
                val joyStickColors = (g2b.reversed() + listOf(Color.GREEN) + g2b).map { it.scale(5) }.apply {
                    forEachIndexed { index, color -> pixelColor(index, color) }
                }
                var lastX = -1
                bus.registerConsumer { x ->
                    val w = (14.0 * x).roundToInt()
                    if (w in (0..14) && w != lastX) {
                        if (lastX != -1) pixelColor(lastX, joyStickColors[lastX])
                        pixelColor(w, Color.YELLOW)
                        lastX = w
                    } else {
                        pixelColor(7, Color.YELLOW)
                    }
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

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty(REMOTE_PI, "marvin.local")
            LEDShim().use {
                it.waitForIt(.5.seconds)
            }
        }
    }
}
