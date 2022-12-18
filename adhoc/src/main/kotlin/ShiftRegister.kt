import crackers.kobots.devices.HC595
import java.lang.Thread.sleep

/*
 * Lessons for 74HC595
 */
// waterfall with multi-segment LED display
fun `lesson 17`() {
    HC595(17, 27, 22).apply {
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

    HC595(17, 27, 22).apply {
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
