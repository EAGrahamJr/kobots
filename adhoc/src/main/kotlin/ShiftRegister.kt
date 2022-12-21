import com.diozero.api.DigitalOutputDevice
import com.diozero.devices.OutputShiftRegister
import crackers.kobots.devices.writeByte
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicInteger

/*
 * Lessons for 74HC595
 */
// waterfall with multi-segment LED display
fun `lesson 17`() {
    OutputShiftRegister(17, 22, 27, 8).apply {
        var x = 0
        2 minutes {
            x = 1
            for (i in 0..7) {
                writeByte(x)
                x = x shl 1
                sleep(100)
            }
            x = 0x80
            for (i in 0..7) {
                writeByte(x)
                x = x shr 1
                sleep(100)
            }
        }
        writeByte(0)
    }
}

// basically copy/pasta from the lesson, but verifies the device is _still_ correct
// note this only makes sense with the circuit for this lesson
val chars_0_9 = listOf(0xc0, 0xf9, 0xa4, 0xb0, 0x99, 0x92, 0x82, 0xf8, 0x80, 0x90)
val chars_a_f = listOf(0x88, 0x83, 0xc6, 0xa1, 0x86, 0x8e)
val decimal = 0x7f

fun `lesson 18`() {
    val chars = chars_0_9 + chars_a_f

    OutputShiftRegister(17, 22, 27, 8).apply {
        2 minutes {
            chars.forEach {
                writeByte(it)
                sleep(1000)
            }

            chars.forEach {
                writeByte(it and decimal)
                sleep(1000)
            }
        }
    }
}

// timing issues that I don't feel like dorking with, but verifies the "writeByte" function
fun `lesson 18 stopwatch`() {
    // right to left, since we're writing least-significant first
    val digits = listOf(DigitalOutputDevice(10, false, false), DigitalOutputDevice(22, false, false), DigitalOutputDevice(27, false, false), DigitalOutputDevice(17, false, false))

    fun selectDigit(digit: Int) {
        digits.forEachIndexed { index, out -> if (digit == index) out.on() else out.off() }
    }

//    val shifter = HC595(24, 23, 18)
    val shifter = OutputShiftRegister(24, 18, 23, 8)

    val clearDisplay = 0xff // ????

    fun writeData(value: Int) {
        selectDigit(0)
//        shifter.writeByte(clearDisplay)
        shifter.writeByte(chars_0_9[value % 10])
        sleep(3)
//        shifter.writeByte(clearDisplay)
//        selectDigit(1)
//        shifter.writeByte(chars_0_9[(value % 100) / 10])
//        sleep(3)
//        shifter.writeByte(clearDisplay)
//        selectDigit(2)
//        shifter.writeByte(chars_0_9[(value % 1000) / 100])
//        sleep(3)
//        shifter.writeByte(clearDisplay)
//        selectDigit(3)
//        shifter.writeByte(chars_0_9[(value % 10_000) / 100])
//        sleep(3)
    }

    val value = AtomicInteger()
    2 minutes {
        println("counter : $value")
        writeData(value.incrementAndGet())
        sleep(1000)
    }
}
