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

import base.stringify
import com.diozero.api.DigitalInputDevice
import com.diozero.api.ServoDevice
import com.diozero.api.ServoTrim
import com.diozero.devices.HCSR04
import com.diozero.devices.HD44780Lcd
import com.diozero.devices.LcdInterface
import crackers.kobots.devices.display.HD44780_Lcd
import crackers.kobots.devices.display.LcdProgressBar
import java.lang.Thread.sleep
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Basic test of integrating multiple elements, including multiple I2C devices, into a single "entity".
 *
 * Notes: switched to `pigpio` provider due to jitter on software PWM servo implementation.
 */
private val KOBOT_NAME = "CatBonker, Mark I"
fun main() {
    val running = AtomicBoolean(true)

    LCD.apply {
        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                running.set(false)
            }
        )
        while (running.get()) {
            timeAndTemp()
            rangeFinder.distanceCm.run {
                servo.reactToDistance(this)
                distance(this)
            }

            heat(irSensor)
            sleep(50)
        }
    }
}

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
        clear()
    }
}

fun HD44780Lcd.timeAndTemp() {
//    val hour = LocalTime.now().hour
//    if (isBacklightEnabled) {
//        if (hour >= 23 || hour < 8) isBacklightEnabled = false
//    } else {
//        if (hour >= 8 && hour < 23) isBacklightEnabled = true
//    }

    val temp = (Files.readAllLines(Paths.get("/sys/class/thermal/thermal_zone0/temp")).first().toFloat() / 1000)
    val time = LocalTime.now().toString().substringBefore(".")

    setCursorPosition(5, 0)
    addText(time)

    setText(1, "Temp: ${temp.stringify(2)}     ")
    addText(if (temp > 48f) 0 else 1)
}

var lastAngle = -1f
fun HD44780Lcd.distance(dist: Float) {
    setText(2, "Dist: ${dist.stringify()} cm  ")
    addText(if (dist < 10) 2.toChar() else ' ')
}

val rangeFinder by lazy { HCSR04(23, 24) }

val irSensor by lazy { DigitalInputDevice(21) }
fun HD44780Lcd.heat(pin: DigitalInputDevice) {
    setText(3, "IR: ")
    addText(if (pin.value) 3.toChar() else ' ')
}

val servo: ServoDevice by lazy { ServoDevice.Builder(17).setTrim(ServoTrim.TOWERPRO_SG90).build() }

// basically flips up the arm of the CatBonker if something gets too close
fun ServoDevice.reactToDistance(dist: Float) {
    (if (dist < 20f) 90f else if (dist < 40) 45f else if (dist < 70f) 15f else 0f).apply {
        if (this != lastAngle) {
            lastAngle = this
            angle = lastAngle
        }
    }
}
