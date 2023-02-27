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
import com.diozero.util.SleepUtil.sleepSeconds
import crackers.kobots.devices.expander.NeoPixel.Companion.neoPixelStrand
import crackers.kobots.utilities.GOLDENROD
import crackers.kobots.utilities.PURPLE
import crackers.kobots.utilities.colorIntervalFromHSB
import crackers.kobots.utilities.kelvinToRGB
import device.examples.RunManager
import kotlinx.coroutines.runBlocking
import java.awt.Color
import java.lang.Thread.sleep

/**
 * TODO fill this in
 */
class CRICKITHatNeoPixels : RunManager() {
    val strand = neoPixelStrand(crickit, 30)

    fun simpleLoop() {
        val warmWhite = 2700.kelvinToRGB()
        while (running.get()) {
            strand.brightness = .01f
            strand.fill(Color.RED)
            sleepSeconds(1)
            strand.fill(Color.GREEN)
            sleepSeconds(1)
            strand.fill(Color.BLUE)
            sleepSeconds(1)
            strand.fill(Color.CYAN)
            sleepSeconds(1)
            strand.fill(PURPLE)
            sleepSeconds(1)
            strand.brightness = .005f
            sleepSeconds(1)
            strand[14] = GOLDENROD
            sleepSeconds(1)
            strand.brightness = .1f
            strand.fill(warmWhite)
            sleepSeconds(1)
            strand.fill(Color.BLACK)
            sleepSeconds(1)
        }
    }

    fun rainbow() {
        strand.brightness = .01f
        colorIntervalFromHSB(0f, 300f, 30).forEachIndexed { index, color ->
            strand[index] = color
        }
        waitForIt()
    }

    fun larson() {
        strand.brightness = .1f
        strand.autoWrite = false
        val otherColor = Color(90, 0, 0)

        var center = -1
        var direction = 1
        var previous = -1
        while (running.get()) {
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
                if (center > 29) {
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

            if (paintItBlack in (0..29)) strand[paintItBlack] = Color.BLACK
            strand[center] = Color.RED
            if (center - 1 >= 0) strand[center - 1] = otherColor
            if (center + 1 < 30) strand[center + 1] = otherColor
            strand.show()
            previous = center
            sleep(30)
        }
        strand.autoWrite = true
    }

    fun execute() = runBlocking {
//            simpleLoop()
//        larson()
        rainbow()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty(REMOTE_PI, "marvin.local")
            CRICKITHatNeoPixels().use {
                it.execute()
                it.strand.fill(Color.BLACK)
            }
        }
    }
}
