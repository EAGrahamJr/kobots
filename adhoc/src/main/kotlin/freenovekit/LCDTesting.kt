package freenovekit

import com.diozero.devices.FNK0079Lcd
import com.diozero.devices.LcdConnection
import com.diozero.devices.LcdInterface
import com.diozero.devices.LcdInterface.Characters
import com.pi4j.LcdDisplay
import minutes
import java.lang.Thread.sleep
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalTime

/**
 * Various dorkings to hack on the LCD 1602 module included in the Freenove kit.
 */
fun `lesson 20`() {
    FNK0079Lcd(LcdConnection.PCF8574LcdConnection(1)).apply {
        displayText("Hello")
        sleep(2000)
        clear()

        1 minutes {
            val temp = (Files.readAllLines(Paths.get("/sys/class/thermal/thermal_zone0/temp")).first().toFloat() / 1000)
            val time = LocalTime.now().toString().substringBefore(".")

            val tempString = String.format("CPU: %.2f", temp)
            setText(0, tempString + (if (temp > 48f) "  HOT" else ""))
            setText(1, time)
            sleep(1000)
        }
    }.close()
}

val chars: List<Byte> = (0..9).map { ('0' + it).code.toByte() } + ('A'..'F').map { it.code.toByte() }
val testString = String(chars.toByteArray())
val hello = "Hello"

val OFF = false
val ON = true

// this is the copy from Pi4J that doesn't quite work all the way
fun lcdLimits() {
    LcdDisplay().apply { // not checked in - modified Pi4J code
        displayText(hello)
        sleep(2000)
        clearDisplay()

        fun doTest(what: String, block: () -> Unit) {
            clearDisplay()
            displayText(what, 1)
            println(what)
            sleep(2000)
            block()
            sleep(5000)
        }

//        doTest("Shift right") {
//            displayText(testString, 0)
//            moveDisplayRight()
//        }
//        doTest("Shift left") {
//            displayText(testString, 0)
//            moveDisplayLeft()
//        }
        doTest("Cursor visible") {
            setCursorToPosition(0, 0)
            setCursorVisibility(OFF)
            "stuff".forEach { writeCharacter(it) }
            sleep(2000)
            setCursorVisibility(ON)
            sleep(2000)
            setCursorBlinking(ON)
        }

//        doTest("Write at") {
//            setCursorToPosition(8, 0)
//            hello.forEach { writeCharacter(it) }
//        }

//        doTest("Cursor move") {
//            moveCursorHome()
//            hello.forEach { writeCharacter(it) }
//            writeCharacter(' ')
//            sleep(2000)
//            moveCursorLeft()
//            sleep(2000)
//            writeCharacter('!')
//            sleep(2000)
//            moveCursorRight()
//            sleep(2000)
//            writeCharacter('+')
//        }

//        doTest("Backlight toggle") {
//            displayText(testString, 0)
//            (1..5).forEach {
//                setDisplayBacklight(OFF)
//                sleep(1000)
//                setDisplayBacklight(ON)
//                sleep(1000)
//            }
//        }

        doTest("Custom character") {
            createCharacter(1, Characters.get("space_invader"))
            createCharacter(2, Characters.get("smilie"))
            createCharacter(0, Characters.get("frownie"))

            writeCharacter(Char(1), 0, 0)
            writeCharacter(Char(2))
            writeCharacter(Char(0))
        }
    }.close()
}

fun lcdFunctionTest(lcd: LcdInterface) {
    lcd.apply {
        setText(0, hello)
        sleep(2000)
        clear()

        fun doTest(what: String, block: () -> Unit) {
            clear()
            setText(1, what)
            println(what)
            sleep(2000)
            block()
            sleep(5000)
        }

        doTest("Shift right") {
            setText(0, testString)
            shiftDisplayRight()
        }
        doTest("Shift left") {
            setText(0, testString)
            shiftDisplayLeft()
        }
        doTest("Cursor visible") {
            setCursorPosition(0, 0)
            cursorOff()
            "stuff".forEach { addText(it) }
            sleep(2000)
            cursorOn()
            sleep(2000)
            blinkOn()
        }

        doTest("Write at") {
            setCursorPosition(8, 0)
            hello.forEach { addText(it) }
        }

        doTest("Cursor move") {
            returnHome()
            hello.forEach { addText(it) }
            addText(' ')
            sleep(2000)
            moveCursorLeft()
            sleep(2000)
            addText('!')
            sleep(2000)
            moveCursorRight()
            sleep(2000)
            addText('+')
        }

        doTest("Backlight toggle") {
            setText(0, testString)
            (1..5).forEach {
                setBacklightEnabled(OFF)
                sleep(1000)
                setBacklightEnabled(ON)
                sleep(1000)
            }
        }
        doTest("Display wrap") {
            displayText("Hello\n   World!!!")
        }
        doTest("Custom character") {
            createChar(1, Characters.get("space_invader"))
            createChar(2, Characters.get("smilie"))
            createChar(0, Characters.get("frownie"))

            setCharacter(0, 0, Char(1))
            addText(Char(2))
            addText(Char(0))
        }
    }.close()
}
