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

import com.diozero.sbc.LocalSystemInfo
import com.diozero.util.SleepUtil
import crackers.kobots.devices.expander.CRICKITHatDeviceFactory
import crackers.kobots.devices.expander.CRICKITNeoPixel
import crackers.kobots.devices.lighting.PimoroniLEDShim
import crackers.kobots.utilities.colorInterval
import crackers.kobots.utilities.scale
import kobots.ops.registerCPUTempConsumer
import org.slf4j.LoggerFactory
import java.awt.Color
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

/**
 * Runs a semi-familiar "chase" pattern based on several TV shows.
 */
private val larsonLogger by lazy { LoggerFactory.getLogger("LarsonDemo") }
fun CRICKITNeoPixel.larson(runFlag: AtomicBoolean, loopDelay: AtomicLong = AtomicLong(30), andThen: () -> Unit = {}) {
    brightness = .1f
    autoWrite = false

    val neopixelMax = size - 1
    val otherColor = Color(90, 0, 0)
    var center = -1
    var direction = 1
    var previous = -1

    larsonLogger.info("Starting")
    while (runFlag.get()) {
        SleepUtil.sleepMillis(loopDelay.get())
        // wipe out previous
        val paintItBlack = if (previous != -1) {
            // heading right, make left-most black
            if (direction > 0) {
                center - 1
            } else {
                center + 1
            }
        } else {
            -1
        }

        if (direction > 0) {
            center++
            if (center > neopixelMax) {
                direction = -1
                center = 28
            }
        } else {
            center--
            if (center < 0) {
                center = 1
                direction = 1
            }
        }

        if (paintItBlack in (0..neopixelMax)) this[paintItBlack] = Color.BLACK
        this[center] = Color.RED
        if (center - 1 >= 0) this[center - 1] = otherColor
        if (center + 1 <= neopixelMax) this[center + 1] = otherColor
        show()
        previous = center
        andThen()
    }
    autoWrite = true
    fill(Color.BLACK)
    larsonLogger.info("Ending")
}

fun PimoroniLEDShim.setupCpuColors(numPixels: Int = this.width): List<Color> = // CPU temperature on the shim
    colorInterval(Color.RED, Color.GREEN, numPixels).map { it.scale(5) }.reversed().apply {
        forEachIndexed { index, color -> pixelColor(index, color) }
    }

/**
 * Pretty specific for the Raspberry Pi temperature range
 */
fun PimoroniLEDShim.flowCPUTemp(numPixels: Int = this.width, pixelOffset: Int = 0) {
    val cpuColors = setupCpuColors(numPixels)
    var lastTemp = -1
    registerCPUTempConsumer { temp ->
        // 30-degree range, offset by 40
        val x = ((temp - 40) * numPixels / 30).roundToInt()
        if (x in (0 until numPixels) && x != lastTemp) {
            if (lastTemp != -1) this[lastTemp + pixelOffset] = cpuColors[lastTemp]
            this[x + pixelOffset] = Color.WHITE
            lastTemp = x
        }
    }
}

fun PimoroniLEDShim.showCPUTemp(numPixels: Int = this.width, pixelOffset: Int = 0): () -> Unit {
    val cpuColors = setupCpuColors(numPixels)
    val systemInfoInstance = LocalSystemInfo.getInstance()

    var lastTemp = -1
    return {
        systemInfoInstance.cpuTemperature.toDouble().let { temp ->
            println("Temp $temp")
            // 30-degree range, offset by 40
            val x = ((temp - 40) * numPixels / 30).roundToInt()
            if (x in (0 until numPixels) && x != lastTemp) {
                if (lastTemp != -1) this[lastTemp + pixelOffset] = cpuColors[lastTemp]
                this[x + pixelOffset] = Color.WHITE
                lastTemp = x
            }
        }
    }
}

// Semi-permanent hookup on the GPIO expander - starting at the non-USB end
// row 1
const val R1_WHITE = 17
const val R1_BLACK = 18

// row 2
const val R2_WHITE = 16
const val R2_PURPLE = 6
const val R2_BLUE = 5
const val R2_GREEN = 25

// row 3 not used
// row 4
const val R4_BROWN = 26
const val R4_RED = 13
const val R4_ORANGE = 21
const val R4_YELLOW = 27
const val R4_BLACK = 4

// use these a lot
val hat by lazy { CRICKITHatDeviceFactory() }
private val stopButton by lazy { hat.touchDigitalIn(4) }

private val running = AtomicBoolean(true)
fun checkStop() = running.let { r ->
    if (stopButton.value) r.compareAndSet(true, false)
    r.get()
}
