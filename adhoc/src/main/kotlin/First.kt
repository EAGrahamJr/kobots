import com.diozero.api.GpioPullUpDown
import com.diozero.devices.Button
import com.diozero.devices.LED
import crackers.kobots.utilities.DebouncedButton
import crackers.kobots.utilities.byName
import crackers.kobots.utilities.defaultPins
import java.time.Duration

fun switchAndLight() {
    val led = LED(17)
    val button = Button(18, GpioPullUpDown.PULL_UP)
    button.whenPressed {
        led.on()
    }
    button.whenReleased {
        led.off()
    }
}

fun myStuff() {
    val ledPin = defaultPins.byName("GPIO17")
    val buttonPin = defaultPins.byName("GPIO18")

    val led = LED(ledPin.deviceNumber)
    val button = Button(buttonPin.deviceNumber, GpioPullUpDown.PULL_UP).apply {
        whenPressed { led.on() }
        whenReleased { led.off() }
    }
}

fun doesItBounce() {
    val led = LED(17)
    var counter = 0
    DebouncedButton(18, Duration.ofMillis(50), GpioPullUpDown.PULL_UP).whenPressed {
        led.toggle()
        println("Counter ${++counter}")
    }
}
