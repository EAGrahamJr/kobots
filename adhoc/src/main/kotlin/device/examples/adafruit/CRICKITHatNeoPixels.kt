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
import crackers.kobots.utilities.GOLDENROD
import crackers.kobots.utilities.PURPLE
import crackers.kobots.utilities.colorIntervalFromHSB
import crackers.kobots.utilities.kelvinToRGB
import device.examples.RunManager
import device.examples.larson
import kobots.ops.createEventBus
import kobots.ops.stopTheBus
import java.awt.Color
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicLong

/**
 * TODO fill this in
 */
class CRICKITHatNeoPixels : RunManager() {
    val strand = crickit.neoPixel(30)

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

    private val larsonDelay = AtomicLong(30)

    fun adjustLarson() {
        createEventBus<Boolean>(name = "Up Larson").apply {
            val touch = crickit.touchDigitalIn(1)
            registerPublisher(errorHandler = errorMessageHandler) {
                touch.value
            }
            registerConditionalConsumer(messageCondition = { it }) {
                larsonDelay.addAndGet(50)
                println("up")
            }
        }
        createEventBus<Boolean>(name = "Down Larson").apply {
            val touch = crickit.touchDigitalIn(2)
            registerPublisher(errorHandler = errorMessageHandler) {
                touch.value
            }
            registerConditionalConsumer(messageCondition = { it }) {
                if (larsonDelay.get() > 50) larsonDelay.addAndGet(-50)
                println("down")
            }
        }
    }

    fun execute() {
//            simpleLoop()
//        adjustLarson()
        strand.larson(running, larsonDelay)
//        rainbow()

        println("Execute done")
        strand.fill(Color.BLACK)
        sleep(100)
        println("Bye now")
        stopTheBus()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty(REMOTE_PI, "marvin.local")
            CRICKITHatNeoPixels().use { it.execute() }
        }
    }
}
