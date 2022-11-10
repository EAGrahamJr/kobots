import com.diozero.api.GpioPullUpDown
import com.diozero.devices.Button
import com.diozero.devices.LED
import crackers.kobots.utilities.byName
import crackers.kobots.utilities.defaultPins

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
    val ledPin = defaultPins.byName("GPIO17")
    val buttonPin = defaultPins.byName("GPIO18")

    val led = LED(ledPin.deviceNumber)
    var counter = 0
    Button(buttonPin.deviceNumber, GpioPullUpDown.PULL_UP).apply {
        whenPressed { led.toggle() }
    }
}
