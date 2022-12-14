import com.diozero.api.DigitalOutputDevice
import com.diozero.api.GpioPullUpDown
import com.diozero.devices.Buzzer
import crackers.kobots.devices.ADS7830
import crackers.kobots.devices.DebouncedButton
import crackers.kobots.devices.GenericMotor
import crackers.kobots.devices.ULN2003Driver
import java.lang.Math.abs
import java.lang.Thread.sleep
import java.time.Duration

/**
 * Super annoying.
 */
fun `lesson 6`() {
    Buzzer(17, true).apply {
        beep(.1f, .2f, 5, false)
    }
}

/**
 * Motor rotor
 */
fun `lesson 13`() {
//    val backup = DigitalOutputDevice(17)
//    val forward = DigitalOutputDevice(27)
//    val speed = PwmOutputDevice(22).apply {
//        pwmFrequency = 100
//    }

//    val motor = TB6612FNGMotor(
//        forward,
//        backup,
//        speed
//    )

    // exactly same as above
    val motor = GenericMotor(27, 17, 22)

    ADS7830.also { adc ->
        5 minutes {
            adc.getValueSafely(0).ifPresent {
                val data = 0.5f - it
                println("Data $data - control ${motor.value}")
                if (abs(data) > 0.05f) motor.value = data * 2f else motor.stop()
            }
            sleep(50)
        }
    }
}

fun `lesson 14`() {
    val button = DebouncedButton(18, Duration.ofMillis(50), GpioPullUpDown.PULL_UP)
    val relayOut = DigitalOutputDevice(17)

    5 minutes {
        button.whenPressed {
            println("Button pressed")
            relayOut.apply {
                if (isOn) off() else on()
            }
        }
    }
}

// fun `lesson 15, which is ignored because PWM sucks`() {
//    val servo = ServoDevice.Builder(defaultPins.byName("GPIO18")).setTrim(TOWERPRO_SG90).build()
//    5 minutes {
//        servo.setValue(-1f)
//        sleep(1000)
//        servo.setValue(0f)
//        sleep(1000)
//        servo.setValue(1f)
//        sleep(1000)
//    }
//    PwmOutputDevice(18, 25, 0f).apply {
//        5 minutes {
//            for (i in 40..100 step 3) {
//                val v = i.toFloat() / 1000f
//                println(v)
//                value = v
//                sleep(2000)
//            }
//        }
//    }
//    ServoDevice.Builder(18).setTrim(TOWERPRO_SG90).build().apply {
//        if (!isOn) println("Device not engaged?")
//
//        for (i in 30..150 step 5) {
//            println("Angle $i")
//            angle = i.toFloat()
//            sleep(1000)
//        }
//    }
// }

fun `lesson 16`() {
    ULN2003Driver(arrayOf(18, 23, 24, 25)).apply {
        5 minutes {
            rotate(360)
            sleep(1000)
            rotate(-360)
            sleep(1000)
        }
    }
}

// had a lego minifig stuck on the end - it was cute
fun `lesson 16 with flair`() {
    ULN2003Driver(arrayOf(18, 23, 24, 25)).apply {
        rotate(-45)
        5 minutes {
            rotate(30)
            rotate(-30)
        }
    }
}
