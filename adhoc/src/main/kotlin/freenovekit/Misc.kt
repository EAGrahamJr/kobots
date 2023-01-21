package freenovekit

import base.minutes
import com.diozero.api.DigitalInputDevice
import com.diozero.api.DigitalOutputDevice
import com.diozero.api.GpioPullUpDown
import com.diozero.api.ServoDevice
import com.diozero.api.ServoTrim.TOWERPRO_SG90
import com.diozero.devices.Buzzer
import com.diozero.devices.HCSR04
import com.diozero.devices.LcdInterface
import com.diozero.devices.imu.invensense.MPU6050
import com.diozero.devices.sandpit.motor.BasicStepperController.GpioFiveWireUnipolarController
import com.diozero.devices.sandpit.motor.Stepper28BYJ48
import crackers.kobots.devices.ADS7830
import crackers.kobots.devices.DebouncedButton
import crackers.kobots.devices.GenericMotor
import crackers.kobots.devices.display.HD44780_Lcd.Pi2Line
import crackers.kobots.devices.display.HD44780_Lcd.Pi4Line
import java.lang.Math.abs
import java.lang.Thread.sleep
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.LocalTime

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

fun `lesson 16`() {
    Stepper28BYJ48(GpioFiveWireUnipolarController(intArrayOf(18, 23, 24, 25))).apply {
        5 minutes {
            rotate(360f)
            sleep(1000)
            rotate(-360f)
            sleep(1000)
        }
    }
}

// had a lego minifig stuck on the end - it was cute
fun `lesson 16 with flair`() {
    val dos = arrayOf(18, 23, 24, 25).map {
        DigitalOutputDevice(it)
    }.toTypedArray()
    Stepper28BYJ48(GpioFiveWireUnipolarController(intArrayOf(18, 23, 24, 25))).apply {
        rotate(-45f)
        5 minutes {
            rotate(30f)
            rotate(-30f)
        }
    }
}

// Live servo test to validate fix in diozero 1.3.5 - validates the servo lesson as well
fun servoFix() {
    ServoDevice.Builder(23).setTrim(TOWERPRO_SG90).build().apply {
        if (!isOn) println("Device not engaged?")

        for (i in 0..180 step 5) {
            println("Angle $i")
            angle = i.toFloat()
            sleep(1000)
        }
        2 minutes {
            angle = 0f
            sleep(1000)
            angle = 90f
            sleep(1000)
            angle = 180f
            sleep(1000)
            angle = 45f
            sleep(1000)
            angle = 135f
            sleep(1000)
        }
    }
}

fun `lesson 24`() {
    val sounder = HCSR04(23, 24)
    Pi2Line.apply {
        createChar(0, LcdInterface.Characters.get("frownie"))
        2 minutes {
            val distance = sounder.distanceCm
            val output = String.format("%.2f", distance)
            setText(0, output)
            if (distance < 10f) setCharacter(15, 0, 0.toChar())
            sleep(10)
        }
    }.close()
    sounder.close()
}

fun `lesson 25`() {
    val sensor = MPU6050(1)

    Pi2Line.apply {
        displayText("Test starting")
        setText(1, "Calibrating...")
        val gyroCalibration = sensor.calibrateGyro(50)
        setText(1, "2 Minutes")
        2 minutes {
            println("a/g ${sensor.accelerometerData}\nd/s ${sensor.gyroData.subtract(gyroCalibration)}")
            setText(0, "Temp ${sensor.temperature}")
            sleep(1000)
        }
    }.close()
    sensor.close()
}

fun `lesson 20`() {
    Pi2Line.apply {
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

fun `lesson 23`() {
    Pi4Line.apply {
        clear()
        cursorOff()
        createChar(0, LcdInterface.Characters.get("heart"))

        val did = DigitalInputDevice(21)
        setText(0, "IR initialized")
        setText(1, "State: ")

        5 minutes {
            setCharacter(7, 1, if (did.value) 0.toChar() else ' ')
        }
    }.close()
}
