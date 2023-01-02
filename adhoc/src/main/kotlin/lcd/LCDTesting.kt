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

package lcd

import com.diozero.devices.LcdConnection
import com.diozero.devices.LcdInterface
import com.diozero.devices.LcdInterface.Characters
import diozero.lcd.SimpleLcd
import java.lang.Thread.sleep

fun FAIL(text: String) = println("$text not supported by hardware")

val chars: List<Byte> = (0..9).map { ('0' + it).code.toByte() } +
        ('A'..'F').map { it.code.toByte() } +
        (1..4).map { ('0' + it).code.toByte() }
val hello = "Hello"

val OFF = false
val ON = true

/**
 * test applied to abstracted interface so everything is the same
 */
fun LcdInterface.lcdFunctionTest() {
    setText(0, hello)
    sleep(2000)
    clear()
    // create the test string to fit the display
    val testString = String(chars.subList(0, this.columnCount).toByteArray())

    fun doTest(testname: String, block: () -> Unit) {
        clear()
        returnHome()
        addText("EdG Test")

        setText(1, testname)
        returnHome()
        println(testname)
        block()
        sleep(2000) // sleep between test
    }

    // skip some?
    doTest("Shifting") {
        addText(testString)
        setCursorPosition(10, 1)
        addText("right")
        shiftDisplayRight()
        sleep(2000)
        setCursorPosition(10, 1)
        addText("left ")
        shiftDisplayLeft()
    }

    if (this is SimpleLcd) {
        doTest("autoscroll") {
            autoscrollOn()
            testString.forEach {
                addText(it)
                sleep(200)
            }
            autoscrollOff()
        }
    }

    doTest("Cursor visible") {
        "stuff ".forEach { addText(it) }
        sleep(500)
        cursorOn()
        blinkOn()
        sleep(2000)
        blinkOff()
        cursorOff()
    }

    if (this is SimpleLcd) {
        doTest("right to left") {
            rightToLeft()
            setCursorPosition(columnCount - 1, 0)
            testString.forEach {
                addText(it)
                sleep(200)
            }
            leftToRight()
        }
    }

    // not in python test
    doTest("Cursor move") {
        returnHome()
        hello.forEach { addText(it) }
        addText(' ')
        sleep(500)
        moveCursorLeft()
        sleep(500)
        addText('!')
        sleep(500)
        moveCursorRight()
        sleep(500)
        addText('+')
    }

    doTest("Custom character") {
        createChar(1, Characters.get("space_invader"))
        createChar(2, Characters.get("smilie"))
        createChar(0, Characters.get("frownie"))

        setCharacter(0, 0, Char(1))
        addText(Char(2))
        addText(Char(0))
    }

    if (rowCount > 2) {
        doTest("Extra lines!!!") {
            setCursorPosition(0, 2)
            addText("Extra lines!")
            setCursorPosition(4, 3)
            addText("go here...")
        }
    }

    doTest("Backlight...") {
        setText(0, testString)
        (1..4).forEach {
            setBacklightEnabled(OFF)
            sleep(1000)
            setBacklightEnabled(ON)
            sleep(1000)
        }
    }
    close()
}

fun backpack(withOutput: Boolean = false): LcdConnection =
    if (withOutput) {
        object : LcdConnection.PCF8574LcdConnection(1) {
            override fun write(values: Byte) {
                super.write(values)
                print(String.format("%02x ", (values.toInt() and 0x00ff)))
            }
        }
    } else {
        LcdConnection.PCF8574LcdConnection(1)
    }

fun main() {
    backpack().use { backpack ->
        // HD
//        HD44780Lcd(backpack, 20, 4).lcdFunctionTest()
//    hackWrapper(HackLcd(4, 20, backpack)).lcdFunctionTest()
        SimpleLcd(backpack, false).lcdFunctionTest()

        // 20x4

        // 16x4
//        HD44780Lcd(backpack, 16, 2).lcdFunctionTest()
//        hackWrapper(HackLcd(2, 16, backpack)).lcdFunctionTest()
//        SimpleLcd(backpack, false).lcdFunctionTest()
    }
}
