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
import crackers.kobots.devices.expander.CRICKITHatDeviceFactory
import crackers.kobots.devices.lighting.PimoroniLEDShim
import device.examples.LEDShimExamples.framesTest
import java.awt.Color
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

object LEDShimExamples {
    val running = AtomicBoolean(true)

    val crickit by lazy {
        CRICKITHatDeviceFactory()
    }

    val zButton by lazy {
        crickit.signalDigitalIn(8)
    }.apply {
    }

    private fun running(): Boolean {
        if (zButton.value) {
            running.set(false)
        }
        return running.get()
    }

    fun PimoroniLEDShim.rainbow() {
        val spacing = 360.0 / 16.0
        while (running()) {
            val hue = ((Instant.now().epochSecond * 100) % 360).toInt()
            for (x in 0 until 28) {
                val offset = x * spacing
                val h = (((hue + offset) % 360) / 360.0).toFloat()
                val rgb = Color.getHSBColor(h, 1f, 1f)
                pixelColor(x, rgb)
            }
            SleepUtil.busySleep(100_000)
        }
    }

    fun PimoroniLEDShim.blinkTest() {
        for (i in 0..9) pixelRGB(i, 100, 0, 0)
        setBlink(1000)
        while (running()) {
            SleepUtil.sleepMillis(1)
        }
        setBlink(0)
    }

    fun PimoroniLEDShim.fillTest() {
        for (i in 1..10) {
            fill(i * 10)
            SleepUtil.sleepSeconds(.5)
        }
        while (running()) {
            SleepUtil.sleepMillis(1)
        }
    }

    fun PimoroniLEDShim.framesTest() {
        println("Setting pixels")
        for (x in 0 until width)
            pixelColor(x, Color.RED.darker().darker(), 0)
        for (x in 0 until width)
            pixelColor(x, Color.GREEN.darker().darker(), 1)
        for (x in 0 until width)
            pixelColor(x, Color.BLUE.darker().darker(), 2)

        println("Running")
        autoPlay(500)
        while (running()) {
//            setFrame(0, true)
//            SleepUtil.sleepSeconds(1)
//            setFrame(1, true)
//            SleepUtil.sleepSeconds(1)
//            setFrame(2, true)
            SleepUtil.sleepSeconds(1)
        }
    }
}

fun main() {
    System.setProperty("diozero.remote.hostname", "marvin.local")

    PimoroniLEDShim().use {
//        it.rainbow()
//        it.blinkTest()
//        it.fillTest()
        it.framesTest()
    }
    LEDShimExamples.crickit.close()
}
