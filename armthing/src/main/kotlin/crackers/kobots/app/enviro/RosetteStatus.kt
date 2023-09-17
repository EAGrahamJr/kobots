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

package crackers.kobots.app.enviro

import com.diozero.sbc.LocalSystemInfo
import crackers.kobots.app.AppCommon.executor
import crackers.kobots.app.crickitHat
import crackers.kobots.utilities.ORANGISH
import java.awt.Color
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/**
 * Blinken lights.
 */
object RosetteStatus {
    private val pixelStatus by lazy { crickitHat.neoPixel(8).apply { brightness = .005f } }
    val systemInfoInstance = LocalSystemInfo.getInstance()

    private val cpuColors = listOf(
        Color.GREEN,
        Color.GREEN,
        Color.GREEN,
        ORANGISH,
        ORANGISH,
        ORANGISH,
        Color.ORANGE,
        Color.RED
    )
    internal val atomicSleep = AtomicBoolean(false)
    lateinit var future: Future<*>

    private val NUM_PIXELS = 8
    private val TEMPERATURE_OFFSET = 40
    private val TEMPERATURE_RANGE = 40

    internal var goToSleep: Boolean
        get() = atomicSleep.get()
        set(value) {
            atomicSleep.set(value)
            if (value) {
                pixelStatus.fill(Color.BLACK)
            } else {
                startColors()
            }
        }

    internal fun stop() {
        pixelStatus.fill(Color.BLACK)
        if (::future.isInitialized) future.cancel(true)
    }

    internal fun start() {
        var lastTemp = -1

        startColors()

        val runner = Runnable {
            if (!goToSleep) {
                systemInfoInstance.cpuTemperature.toDouble().let { temp ->
                    val x = ((temp - TEMPERATURE_OFFSET) * NUM_PIXELS / TEMPERATURE_RANGE).roundToInt()
                    if (x in (0 until NUM_PIXELS) && x != lastTemp) {
                        if (lastTemp != -1) pixelStatus[lastTemp] = cpuColors[lastTemp]
                        pixelStatus[x] = Color.WHITE
                        lastTemp = x
                    }
                }
            }
        }

        future = executor.scheduleAtFixedRate(runner, 0, 15, TimeUnit.SECONDS)
    }

    private fun startColors() {
        cpuColors.forEachIndexed { index, color -> pixelStatus[index] = color }
    }
}
