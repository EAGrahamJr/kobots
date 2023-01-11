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

package dork

import com.diozero.api.DigitalInputDevice
import com.diozero.api.ServoDevice
import com.diozero.api.ServoTrim
import com.diozero.devices.HCSR04
import com.diozero.devices.LcdInterface
import com.diozero.devices.PwmLed
import com.diozero.util.SleepUtil.busySleep
import com.diozero.util.SleepUtil.sleepSeconds
import crackers.kobots.devices.at
import crackers.kobots.devices.display.HD44780_Lcd
import crackers.kobots.devices.display.LcdProgressBar
import crackers.kobots.devices.plusAssign
import crackers.kobots.devices.position
import crackers.kobots.devices.set
import java.lang.Thread.sleep
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Basic test of integrating multiple elements, including multiple I2C devices, into a single "entity".
 *
 * Notes: switched to `pigpio` provider due to increased CPU temp on software PWM servo implementation.
 */
private val KOBOT_NAME = "CatBonker, Mk I, v2"

// inputs
val rangeFinder by lazy { HCSR04(23, 24) }
val irSensor by lazy { DigitalInputDevice(21) }

// outputs
val servo: ServoDevice by lazy { ServoDevice.Builder(17).setTrim(ServoTrim.TOWERPRO_SG90).build() }
val led by lazy { PwmLed(27) }

val LCD by lazy {
    HD44780_Lcd.Pi4Line.apply {
        cursorOff()
        blinkOff()
        println(KOBOT_NAME)
        displayText(KOBOT_NAME)
        LcdProgressBar(this, 1).also { pb ->
            (0..100).forEach {
                pb.value = it
                sleep(25)
            }
        }
        createChar(0, LcdInterface.Characters.get("frownie"))
        createChar(1, LcdInterface.Characters.get("smilie"))
        createChar(2, LcdInterface.Characters.get("space_invader"))
        createChar(3, LcdInterface.Characters.get("heart"))
    }
}

// RUN!
fun main() {
    val running = AtomicBoolean(true)

    LCD.use { lcd ->
        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                running.set(false)
            }
        )
        while (running.get()) {
            LocalTime.now().also { time ->
                if (time.hour >= 23 || time.hour < 8) {
                    // nighttime: turn it off and send the pointer "home"
                    if (lcd.isBacklightEnabled) {
                        lcd.isBacklightEnabled = false
                        servo set 0f
                        led set .1f
                    }
                    sleepSeconds(300)
                } else {
                    // SHOWTIME!
                    with(lcd) {
                        if (!isBacklightEnabled) isBacklightEnabled = true
                    }

                    LCD position Pair(1, 6)
                    LCD += time.toString().substringBefore(".")

                    showTemp()

                    val distanceCm = rangeFinder.distanceCm
                    LCD[3] = "Dist: ${distanceCm.toInt()} cm    " + if (distanceCm < 10f) 2.toChar() else ' '

                    // reacts if "too close" otherwise, hour of day-ish
                    servo at (if (distanceCm < 20f) 90f else ((LocalTime.now().hour / 24f) * 180f))
                    // lights up if triggered
                    led set irSensor.value

                    busySleep(1_000_000) // 1 ms
                }
            }
        }
    }
}

fun showTemp() {
    val temp = (Files.readAllLines(Paths.get("/sys/class/thermal/thermal_zone0/temp")).first().toFloat() / 1000).toInt()

    LCD[2] = "Temp: $temp      " + (if (temp > 56f) 0 else 1).toChar()
}
