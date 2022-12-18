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

import com.diozero.api.DigitalOutputDevice
import java.lang.Thread.sleep

/**
 * [74HC595](https://www.ti.com/lit/gpn/sn74hc595) shift register: transmits 8-bit data serially, MSB first.
 *
 * * [data] is the DS pin (14)
 * * [latch] is the ST_CP pin (12)
 * * [clock] is the CH_CP pin (11)
 *
 * TODO variable waits for the various line toggles?
 */
class HC595(data: Int, latch: Int, clock: Int) {
    val dataPin = DigitalOutputDevice(data)
    val latchPin = DigitalOutputDevice(latch)
    val clockPin = DigitalOutputDevice(clock)

    private val MSB = 0x80
    private val CLOCK_WAIT = 1L

    /**
     * Write a byte-value to the device, in bit-order (e.g. bit 0 corresponds to Q0, bit 7 to Q7)
     */
    fun writeByte(value: Int) {
        if (value > 255) throw IllegalArgumentException("Value must be byte-sized")

        // wait for it
        latchPin.off()
        (0..7).forEach { bit: Int ->
            // wait to store
            clockPin.off()
            // yes/no
            if (MSB and (value shl bit) == MSB) dataPin.on() else dataPin.off()
            sleep(CLOCK_WAIT)
            // store it
            clockPin.on()
            sleep(CLOCK_WAIT)
        }
        // send it
        latchPin.on()
    }
}
