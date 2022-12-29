/*
 * Copyright 2022-2022 by E. A. Graham, Jr.
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

package freenovekit

import com.diozero.api.GpioPullUpDown
import com.diozero.devices.*
import crackers.kobots.devices.AnodeRgbPwmLed
import crackers.kobots.devices.DebouncedButton
import crackers.kobots.utilities.byName
import crackers.kobots.utilities.defaultPins
import java.awt.Color
import java.lang.Thread.sleep
import java.time.Duration

fun `lesson 2`() {
    val led = LED(17)
    val button = Button(18, GpioPullUpDown.PULL_UP)
    button.whenPressed {
        led.on()
    }
    button.whenReleased {
        led.off()
    }
}

fun `lesson 2 with my stuff`() {
    val ledPin = defaultPins.byName("GPIO17")
    val buttonPin = defaultPins.byName("GPIO18")

    val led = LED(ledPin.deviceNumber)
    val button = Button(buttonPin.deviceNumber, GpioPullUpDown.PULL_UP).apply {
        whenPressed { led.on() }
        whenReleased { led.off() }
    }
}

fun `lesson 2 - debounce`() {
    val led = LED(17)
    var counter = 0
    DebouncedButton(18, Duration.ofMillis(50), GpioPullUpDown.PULL_UP).whenPressed {
        led.toggle()
        println("Counter ${++counter}")
    }
}

fun `lesson 4`() {
    PwmLed(17).apply {
        pulse(2.0f, 256, 20, false)
    }
}

fun `lesson 5`() {
    // my anode LED is backwards
    RgbLed(17, 18, 27).use {
        it.setValues(false, true, false)
        sleep(5000)
        it.setValues(true, false, true)
        sleep(5000)
        it.setValues(true, true, true)
        sleep(1000)
    }
}

fun `lesson 5 with my stuff`() {
    AnodeRgbPwmLed(17, 18, 27).let { led ->
        (0..10 step 2).forEach { red ->
            (0..10 step 2).forEach { green ->
                (0..10 step 2).forEach { blue ->
                    led.setValues(red / 10f, green / 10f, blue / 10f)
                    sleep(500)
                }
            }
        }
        println("off-ish?")
        led.off()
        sleep(1000)
    }
}

fun `rainbow because`() {
    // awt colors do not translate to RGB LED because not enough juice?
    val colors = listOf(
        Color.red,
        Color(1f, .2f, 0f),
        Color(1f, .33f, 0f),
        Color(0f, .8f, 0f),
        Color(0f, 0f, .8f),
        Color(1f, 0f, .8f)
    )
    AnodeRgbPwmLed(17, 27, 22).let { led ->
        (1..10).forEach {
            colors.forEach { color ->
                led.setColor(color)
                sleep(3000)
            }
        }
    }
}

val lesson3Pins = listOf(17, 18, 27, 22, 23, 24, 25, 2, 3, 8)
fun `lesson 3`() {
    val leds = lesson3Pins.map { LED(it) }
    (1..10).forEach {
        leds[it - 1].on()
        sleep(3000)
        leds[it - 1].off()
    }
    LedBarGraph(leds).let { bg ->
        (1..10).forEach {
            bg.value = it / 10f
            sleep(2000)
        }
        bg.off()
    }
}

/**
 * Because of the way the pulse is implemented, this has a
 * really neat, flowing effect as more segments are added.
 */
fun `lesson 3 with pwm evil grin`() {
    lesson3Pins.map { PwmLed(it) }.let { list ->
        list.forEach {
            it.pulse()
            sleep(2000)
        }
        sleep(5000)
        list.forEach { it.off() }
    }
}
