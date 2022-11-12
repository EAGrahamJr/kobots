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
import com.diozero.api.GpioPullUpDown
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
