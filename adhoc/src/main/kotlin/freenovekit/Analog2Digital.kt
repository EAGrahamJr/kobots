package freenovekit

import com.diozero.api.I2CDevice
import crackers.kobots.devices.AnodeRgbPwmLed
import crackers.kobots.devices.asDegreesF
import crackers.kobots.devices.expander.ADS7830
import crackers.kobots.utilities.elapsed
import java.lang.Thread.sleep
import java.time.Instant

fun `lesson 7`() {
    ADS7830().apply {
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
    ADS7830().apply {
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
    ADS7830().apply {
        val start = Instant.now()
        while (start.elapsed().seconds < 300) {
            println(getTemperature(getValue(0).toDouble()).asDegreesF())
        }
    }
}

fun `probe i2c`() {
    ADS7830().apply {
        println("is it there? ${i2CDevice.probe()}")
    }
    println("Other one there? ${I2CDevice(1, 0x48).probe()}")
    sleep(100)
}

const val KELVIN = 273.15f

/**
 * A function that converts a percentage reading into Celsius temperature for a "generic" thermistor.
 *
 * The formula given is `T2 = 1/(1/T1 + ln(Rt/R1)/B)`
 */
fun getTemperature(value: Double, referenceVoltage: Float = 3.3f): Float {
    val voltage = referenceVoltage * value
    // NOTE: this would normally be multiplied by the reference resistence (R1) to get the current, but that is divided back out in the formula
    val voltageRatio = voltage / (referenceVoltage - voltage)
    // 25 is the "reference" temperature
    // 3950 is the "thermal index"
    val k = 1 / (1 / (KELVIN + 25) + Math.log(voltageRatio) / 3950.0)
    println("V $voltage R $voltageRatio K $k")
    return (k - KELVIN).toFloat()
}
