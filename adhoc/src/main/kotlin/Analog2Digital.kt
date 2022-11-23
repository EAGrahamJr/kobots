import crackers.kobots.devices.ADS7830
import crackers.kobots.devices.AnodeRgbPwmLed
import crackers.kobots.utilities.elapsed
import java.lang.Thread.sleep
import java.time.Instant

fun `lesson 7`() {
    ADS7830.apply {
        val start = Instant.now()
        while (start.elapsed().seconds < 300) {
            val data = getValue(0)
            println("Voltage ${data * 3.3f}")
            sleep(100)
        }
    }
}

fun `lesson 9 but using my stuff`() {
    val rgb = AnodeRgbPwmLed(17, 27, 22)
    ADS7830.apply {
        val start = Instant.now()
        while (start.elapsed().seconds < 300) {
            val red = getValue(0)
            val green = getValue(1)
            val blue = getValue(2)
            println("RGB = $red, $green, $blue")
            rgb.setValues(red, green, blue)
            sleep(100)
        }
    }
}

fun `lesson 11`() {
    ADS7830.apply {
        val start = Instant.now()
        while (start.elapsed().seconds < 300) {
            println(getTemperature(0).asDegreesF())
        }
    }
}
