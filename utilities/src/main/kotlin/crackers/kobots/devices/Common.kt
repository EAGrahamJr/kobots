/*
 * Copyright 2022-2022 by E. A. Graham, Jr.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package crackers.kobots.devices

import com.diozero.api.DebouncedDigitalInputDevice
import com.diozero.api.DigitalOutputDevice
import com.diozero.api.GpioPullUpDown
import com.diozero.api.PwmOutputDevice
import com.diozero.devices.motor.TB6612FNGMotor
import com.diozero.internal.spi.GpioDeviceFactoryInterface
import crackers.kobots.utilities.nativeDeviceFactory
import java.time.Duration
import java.util.function.LongConsumer

/**
 * Simple extension to get the `whenPressed` and `whenReleased` semantics with debounce.
 */
class DebouncedButton @JvmOverloads constructor(
    gpio: Int,
    debounceTime: Duration,
    pud: GpioPullUpDown = GpioPullUpDown.NONE,
    activeHigh: Boolean = pud != GpioPullUpDown.PULL_UP,
    deviceFactory: GpioDeviceFactoryInterface = nativeDeviceFactory
) : DebouncedDigitalInputDevice(deviceFactory, deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(gpio), pud, activeHigh, debounceTime.toMillis().toInt()) {

    /**
     * Action to perform when the button is pressed.
     * @param buttonConsumer Callback function to invoke when pressed (long parameter is nanoseconds time).
     */
    fun whenPressed(buttonConsumer: LongConsumer) {
        whenActivated(buttonConsumer)
    }

    /**
     * Action to perform when the button is released.
     * @param buttonConsumer Callback function to invoke when released (long parameter is nanoseconds time).
     */
    fun whenReleased(buttonConsumer: LongConsumer) {
        whenDeactivated(buttonConsumer)
    }
}

/**
 * Opinionated wrapper around TB6612FNGMotor (sets freq. to 100)
 *
 * Based on a similar driver: [L293D](https://www.ti.com/lit/gpn/l293d)
 **/
class GenericMotor(forwardPin: Int, backwardPin: Int, enablePin: Int, frequency: Int = 100) :
    TB6612FNGMotor(DigitalOutputDevice(forwardPin), DigitalOutputDevice(backwardPin), PwmOutputDevice(enablePin).apply { pwmFrequency = frequency })

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

/**
 * Convert a float representing degrees C to F.
 */
fun Float.asDegreesF(): Float = (this * 9f / 5f) + 32.0f
