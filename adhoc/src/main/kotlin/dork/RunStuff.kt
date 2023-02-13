/*
 * Copyright 2022-2023 by E. A. Graham, Jr.
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

package dork

import crackers.kobots.devices.expander.CRICKITHat
import crackers.kobots.utilities.hex

fun main() {
    System.setProperty("diozero.remote.hostname", "marvin.local")

    CRICKITHat().use { hat ->
        println("Chip ${hat.seeSaw.chipId.hex()}")
//        hat.booleanTests()
//        analogTests()
//        signalAnalog()
//        dorking(hat)
    }
}

// this was with the original pieces
/*
fun dorking(crickit: CRICKITHat) {
    crickit.use { hat ->
        val noodle = hat.signal(8).apply {
            mode = SignalMode.OUTPUT
        }
        val blueLed = hat.signal(7).apply {
            mode = SignalMode.OUTPUT
        }
        val greenLed = hat.signal(6).apply {
            mode = SignalMode.OUTPUT
        }
        val yellowLed = hat.signal(5).apply {
            mode = SignalMode.OUTPUT
        }
        val redLed = hat.signal(4).apply {
            mode = SignalMode.OUTPUT
        }
        val greenButton = hat.signal(3).apply {
            mode = SignalMode.INPUT_PULLUP
        }
        val yellowButton = hat.signal(2).apply {
            mode = SignalMode.INPUT_PULLUP
        }
        val redButton = hat.signal(1).apply {
            mode = SignalMode.INPUT_PULLUP
        }
        val touch4 = hat.touch(4)

        fun stopAll() {
            noodle.value = false
            greenLed.value = false
            blueLed.value = false
            yellowLed.value = false
            redLed.value = false
        }

        1 minutes {

            val r = redButton.value
            val g = greenButton.value
            val y = yellowButton.value
            redLed.value = r
            yellowLed.value = y
            greenLed.value = g
            blueLed.value = g and y
            noodle.value = r and y

            sleep(300)
            touch4.isOn.let {
                if (it) {
                    stopAll()
                    exitProcess(1)
                }
            }
        }
        stopAll()
    }
}

fun CRICKITHat.booleanTests() {
    var lastValue = false
    signal(8).let { noodle ->
        noodle.mode = SignalMode.OUTPUT

        val otherTouch = touch(4)

//        touch(1).let { touch ->
//            val f = { touch.isOn }
//        DigitalInputDevice(this, CRICKITHatDeviceFactory.Types.TOUCH.deviceNumber(1)).let { d ->
//            val f = { d.value}
        signal(3).let { s ->
            s.mode = SignalMode.INPUT_PULLUP
            val f = { s.value }

            1 minutes {
                val read = f()
                println("read $read")
                if (read != lastValue) {
                    noodle.value = read
                    lastValue = read
                }
                println("4 value is ${otherTouch.value}")
                sleep(500)
            }
        }
    }
}

//fun CRICKITHat.analogTouchTests() {
//    AnalogInputDevice(this, CRICKITHat.Types.TOUCH.deviceNumber(1)).use { d ->
//        1 minutes {
//            println("Unscaled = ${d.unscaledValue}, scaled = ${d.scaledValue}")
//            sleep(500)
//        }
//    }
//}

fun CRICKITHat.signalAnalog() {
    signal(8).let { s ->
        s.mode = SignalMode.INPUT
        signal(3).let { noodle ->
            noodle.mode = SignalMode.OUTPUT
            1 minutes {
                val theValue = s.read()
                println("Read $theValue")
                noodle.value = theValue > 512
                sleep(200)
            }
        }
    }
}
*/
