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

package dork

import com.diozero.devices.LcdInterface
import crackers.kobots.devices.display.HD44780_Lcd.Pi2Line
import crackers.kobots.devices.display.HD44780_Lcd.Pi4Line
import crackers.kobots.devices.display.LcdProgressBar
import minutes
import java.lang.Thread.sleep
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalTime

fun main() {
    kobotDisplay()
//    validChars()
}

// fun `put me down`() {
//    val sensor = MPU6050(1)
//    TC1604Lcd().apply {
//        displayText("Test starting")
// //        setText(1, "Calibrating...")
// //        val gyroCalibration = sensor.calibrateGyro(50)
//
//        var lastZ = sensor.accelerometerData.z
//        2 minutes {
//            val nowZ = sensor.accelerometerData.z
//            val diff = nowZ - lastZ
//            lastZ = nowZ
//            setText(1, "Diff ${String.format("%+.2f", diff)}")
//            sleep(500)
//        }
//    }.close()
//    sensor.close()
// }

fun shiftSilly() {
    sleep(1000)
    Pi2Line.apply {
        createChar(0, LcdInterface.Characters.get("smilie"))
        setText(0, "Hi, Roger! " + 0.toChar())
        sleep(2000)
        for (i in 1..16) {
            shiftDisplayLeft()
            sleep(250)
        }
        clear()
        returnHome()
        displayText("Wanna do lunch\nnext week?")
        sleep(3000)
    }.close()
}

fun kobotDisplay() {
    Pi4Line.apply {
        cursorOff()
        blinkOff()
        displayText("Lasers charging")
        createChar(0, LcdInterface.Characters.get("frownie"))
        createChar(1, LcdInterface.Characters.get("smilie"))

        LcdProgressBar(this, 1).also { pb ->
            (0..100).forEach { progress ->
                pb.value = progress
                sleep(100)
            }
        }
        clear()

        1 minutes {
            val temp = (Files.readAllLines(Paths.get("/sys/class/thermal/thermal_zone0/temp")).first().toFloat() / 1000)
            val time = LocalTime.now().toString().substringBefore(".")

            val tempString = String.format("Temp: %.2f ", temp)
            setText(0, tempString)
            addText(if (temp > 48f) 0 else 1)
            setCursorPosition(5, 1)
            addText(time)
            sleep(1000)
        }
    }.close()
}

fun validChars() {
    var count = 0

    val names = LcdInterface.Characters.getCharacters()
    names.forEach { name ->
        if (LcdInterface.Characters.get(name).size < 8) {
            count++
        } else {
            println("Avail - $name")
        }
    }
    println(" ${names.size} Characters, $count too small")
}
