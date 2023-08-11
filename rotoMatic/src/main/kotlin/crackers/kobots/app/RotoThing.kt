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

package crackers.kobots.app

import com.diozero.api.ServoDevice
import com.diozero.api.ServoTrim
import crackers.kobots.devices.io.GamepadQT
import crackers.kobots.parts.ServoRotator
import crackers.kobots.utilities.KobotSleep
import kotlin.system.exitProcess
import com.diozero.devices.PCA9685 as RobotHat

/**
 * RotoMatic uses a Rotator to select a thing. Manual selections are through something with buttons.
 */

private val hat by lazy {
    RobotHat()
}

val servo by lazy {
    ServoDevice
        // gpio style: need to use hardware GPIO
        .Builder(18)
        // hat style
//         .Builder(15).setDeviceFactory(hat)
        .setTrim(ServoTrim.TOWERPRO_SG90)
        .build().apply { angle = 10.0f }
}

private val rotator by lazy {
    ServoRotator(servo, (0..360), (10..180))
}

private val stopList = listOf(0, 61, 122, 184, 260)
private val gamepad by lazy { GamepadQT() }

const val REMOTE_PI = "diozero.remote.hostname"
const val PSYCHE = "psyche.local"

fun main() {
//    System.setProperty(REMOTE_PI, PSYCHE)

    // these do not require the CRICKIT
    gamepad.use { pad ->
        var done = false
        var stopIndex = 0
        while (!done) {
            pad.read().apply {
                when {
                    a -> {
                        rotator.swing(stopList[stopIndex++ % stopList.size])
                    }

                    b -> {
                        stopIndex--
                        if (stopIndex < 0) stopIndex = stopList.size - 1
                        rotator.swing(stopList[stopIndex])
                    }

                    select -> println("Current angle ${rotator.current()}")
                    start -> done = true
                    else -> {}
                }
            }
            KobotSleep.millis(50)
        }
    }
    exitProcess(0)
}

private fun ServoRotator.swing(target: Int) {
    while (!rotateTo(target)) {
        KobotSleep.millis(50)
    }
}
