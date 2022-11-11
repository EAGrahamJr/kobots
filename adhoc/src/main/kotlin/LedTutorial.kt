import com.diozero.api.GpioPullUpDown
import com.diozero.devices.Button
import com.diozero.devices.LED
import com.diozero.devices.PwmLed
import com.diozero.devices.RgbLed
import crackers.kobots.utilities.DebouncedButton
import crackers.kobots.utilities.byName
import crackers.kobots.utilities.defaultPins
import java.lang.Thread.sleep
import java.time.Duration
import kotlin.system.exitProcess

/**
 * Tutorials from Freenove "ultimate" Raspberry Pi starter set
 */
fun `lesson 2`() {
    val led = LED(17)
    val button = Button(18, GpioPullUpDown.PULL_UP)
    button.whenPressed {
        led.on()
    }
    button.whenReleased {
        led.off()
    }
}

fun `lession 2 with my stuff`() {
    val ledPin = defaultPins.byName("GPIO17")
    val buttonPin = defaultPins.byName("GPIO18")

    val led = LED(ledPin.deviceNumber)
    val button = Button(buttonPin.deviceNumber, GpioPullUpDown.PULL_UP).apply {
        whenPressed { led.on() }
        whenReleased { led.off() }
    }
}

fun `lesson 2 - debounce`() {
    val led = LED(17)
    var counter = 0
    DebouncedButton(18, Duration.ofMillis(50), GpioPullUpDown.PULL_UP).whenPressed {
        led.toggle()
        println("Counter ${++counter}")
    }
}

fun `lesson 4`() {
    PwmLed(17).apply {
        pulse(2.0f,256,20,false)
        exitProcess(0)
    }
}

fun `lesson 5`() {
    RgbLed(17,18,27).use{
        it.setValues(false, true, false)
        sleep(15000)
        it.setValues(true, true, true)
        exitProcess(0)
    }
}
