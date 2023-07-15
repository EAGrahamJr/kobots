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

import base.MARVIN
import base.REMOTE_PI
import com.diozero.util.SleepUtil.sleepSeconds
import crackers.kobots.devices.expander.CRICKITHat
import crackers.kobots.utilities.GOLDENROD
import crackers.kobots.utilities.PURPLE
import crackers.kobots.utilities.colorIntervalFromHSB
import crackers.kobots.utilities.kelvinToRGB
import device.examples.larson
import java.awt.Color
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 *
 */
class CRICKITHatNeoPixels : AutoCloseable {
    val running: AtomicBoolean = AtomicBoolean(true)

    val crickit by lazy { CRICKITHat() }
    val strand by lazy { crickit.neoPixel(30) }
    val stopIt by lazy {
        crickit.touchDigitalIn(4).apply {
            value
        }
    }
    val touchUp by lazy { crickit.touchDigitalIn(1) }
    val touchDown by lazy { crickit.touchDigitalIn(2) }

    override fun close() {
        crickit.close()
    }

    fun simpleLoop() {
        val warmWhite = 2700.kelvinToRGB()
        while (running.get()) {
            strand.brightness = .1f
            strand + Color.RED
            sleepSeconds(1)
            strand + Color.GREEN
            sleepSeconds(1)
            strand + Color.BLUE
            sleepSeconds(1)
            strand + Color.CYAN
            sleepSeconds(1)
            strand + PURPLE
            sleepSeconds(1)
            sleepSeconds(1)
            strand[14] = GOLDENROD
            sleepSeconds(1)
            strand.brightness = .1f
            strand + warmWhite
            sleepSeconds(1)
            strand.off()
            sleepSeconds(1)
            println("up ${touchUp.value} down ${touchDown.value} ${stopIt.value}")
            if (stopIt.value) break
        }
    }

    fun rainbow() {
        strand.brightness = .1f
        colorIntervalFromHSB(0f, 300f, 30).forEachIndexed { index, color ->
            strand[index] = color
        }
    }

    private val larsonDelay = AtomicLong(30)

    fun adjustLarson() {
        if (touchUp.value) {
            larsonDelay.addAndGet(50)
            println("up")
        } else if (touchDown.value) {
            if (larsonDelay.get() > 50) larsonDelay.addAndGet(-50)
            println("down")
        }
    }

    fun execute() {
        println("Simple loop")
        simpleLoop()
        sleep(2000)
        running.set(true)
        println("LARSON!")
        strand.larson(running, larsonDelay) {
            adjustLarson()
            if (stopIt.value) running.set(false)
        }
        sleep(2000)
        println("Rainbow")
        rainbow()
        sleep(10000)

        println("Execute done")
        strand.off()
//        sleep(100)
        println("Bye now")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty(REMOTE_PI, MARVIN)
            CRICKITHatNeoPixels().use { it.execute() }
        }
    }
}
