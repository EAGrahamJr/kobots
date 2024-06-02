/*
 * Copyright 2022-2024 by E. A. Graham, Jr.
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

package crackers.kobots.app.otherstuff

import com.diozero.api.DigitalOutputDevice
import com.diozero.api.PwmOutputDevice

/**
 * GPIO (GP == Jeep)
 */
object Jeep {
    val noodleLamp = PwmOutputDevice(4)
    val stepper1Pins = listOf(
        DigitalOutputDevice(17),
        DigitalOutputDevice(18),
        DigitalOutputDevice(10),
        DigitalOutputDevice(9)
    )
}
