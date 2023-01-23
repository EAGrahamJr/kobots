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

package qwiic

import base.minutes
import crackers.kobots.devices.qwiicKill
import crackers.kobots.devices.sensors.VCNL4040
import java.lang.Thread.sleep
import kotlin.system.exitProcess

fun main() {
    qwiicKill.whenPressed {
        println("Holy Abort Button, Batman!")
        exitProcess(2)
    }
    VCNL4040(1).use {
        it.dumpRegisters()

        println("Turn on")
        it.enableProximity(true)
        it.enableAmbientLight(true)
        it.enableWhiteLight(true)
        it.proximityHighResolution = VCNL4040.ProximityResolution.PS_RES_16

        15 minutes {
            sleep(500)
            println("Light ${it.ambientLight} (lux ${it.luminosity}), White value ${it.whiteLight}")
            println("Prox ${it.proximity}")
        }
    }
}
