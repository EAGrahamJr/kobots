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

package device.examples.qwiic

import base.minutes
import crackers.kobots.devices.qwiicKill
import crackers.kobots.devices.sensors.VCNL4040
import crackers.kobots.devices.sensors.VL6180X
import java.lang.Thread.sleep
import kotlin.system.exitProcess

fun main() {
    System.setProperty("diozero.remote.hostname", "useless.local")

    qwiicKill.whenPressed {
        println("Holy Abort Button, Batman!")
        exitProcess(2)
    }
    ambientLightAndProximity()
}

private fun ambientLightAndProximity() {
    VCNL4040().use {
        it.dumpRegisters()

        println("Turn on")
        it.proximityEnabled = true
        it.ambientLightEnabled = true
        it.whiteLightEnabled = true

        15 minutes {
            sleep(500)
            println("Light ${it.ambientLight} (lux ${it.luminosity}), White value ${it.whiteLight}")
            println("Prox ${it.proximity}")
        }
    }
}

fun timeOfFlight() {
    VL6180X().use { sensor ->
        2 minutes {
            println("Distance: ${sensor.range}")
            val lux1 = sensor.getLuminosity(VL6180X.Companion.ALSGain.GAIN_1)
            val lux10 = sensor.getLuminosity(VL6180X.Companion.ALSGain.GAIN_10)
            println("Lux 1x - $lux1 10x - $lux10")
            sleep(1000)
        }
    }
}
