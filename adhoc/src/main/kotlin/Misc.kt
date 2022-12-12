import com.diozero.api.DigitalOutputDevice
import com.diozero.api.GpioPullUpDown
import com.diozero.devices.Buzzer
import crackers.kobots.devices.ADS7830
import crackers.kobots.devices.DebouncedButton
import crackers.kobots.devices.GenericMotor
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
        run5minutes {
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

    run5minutes {
        button.whenPressed {
            println("Button pressed")
            relayOut.apply {
                if (isOn) off() else on()
            }
        }
    }
}
