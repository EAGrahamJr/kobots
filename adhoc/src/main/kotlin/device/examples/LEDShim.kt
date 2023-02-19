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

import com.diozero.util.SleepUtil
import crackers.kobots.devices.lighting.PimoroniLEDShim
import crackers.kobots.utilities.colorInterval
import crackers.kobots.utilities.colorIntervalFromHSB
import crackers.kobots.utilities.scale
import device.examples.LEDShimExamples.framesTest
import java.awt.Color
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


object LEDShimExamples : RunManager by DefaultRunManager() {

    fun PimoroniLEDShim.rainbow() {
        val spacing = 360.0 / 16.0
        waitForIt {
            val hue = ((Instant.now().epochSecond * 100) % 360).toInt()
            for (x in 0 until 28) {
                val offset = x * spacing
                val h = (((hue + offset) % 360) / 360.0).toFloat()
                val rgb = Color.getHSBColor(h, 1f, 1f)
                pixelColor(x, rgb)
            }
        }
    }

    fun PimoroniLEDShim.blinkTest() {
        for (i in 0..9) pixelRGB(i, 100, 0, 0)
        setBlink(1000)
        waitForIt()
        setBlink(0)
    }

    fun PimoroniLEDShim.fillTest() {
        for (i in 1..10) {
            fill(i * 10)
            SleepUtil.sleepSeconds(.5)
        }
        waitForIt(250.milliseconds)
    }

    fun PimoroniLEDShim.framesTest() {
        println("Setting pixels")
        for (x in 0 until width)
            pixelColor(x, Color.RED.scale(15), 0)
        for (x in 0 until width)
            pixelColor(x, Color.GREEN.scale(15), 1)
        for (x in 0 until width)
            pixelColor(x, Color.BLUE.scale(15), 2)

        println("Running")
        autoPlay(500, frames = 3)
        waitForIt(1.seconds) {
//            setFrame(0, true)
//            SleepUtil.sleepSeconds(1)
//            setFrame(1, true)
//            SleepUtil.sleepSeconds(1)
//            setFrame(2, true)
        }
    }

    fun PimoroniLEDShim.hueRange() {
        setFrame(2, true)
        colorIntervalFromHSB(0f, 240f, width).forEachIndexed { index, color ->
            pixelColor(index, color, 0)
        }
        colorInterval(Color.RED, Color.BLUE, width).forEachIndexed { index, color ->
            pixelColor(index, color, 1)
        }

        waitForIt(1.seconds) {
            setFrame(0, true)
            SleepUtil.sleepSeconds(1)
            setFrame(1, true)
        }
    }
}

fun main() {
    System.setProperty("diozero.remote.hostname", "marvin.local")
    LEDShimExamples.use {
        PimoroniLEDShim().use {
//            it.rainbow()
//        it.blinkTest()
//        it.fillTest()
            it.framesTest()
//        it.hueRange()
        }
    }
}
