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
import crackers.kobots.devices.at
import crackers.kobots.devices.io.GamepadQT
import crackers.kobots.mqtt.KobotsMQTT
import crackers.kobots.mqtt.KobotsMQTT.Companion.BROKER
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
    // gpio style: need to use hardware GPIO
    val builder =
//        ServoDevice.Builder(18)
        // hat style
        ServoDevice.Builder(0).setDeviceFactory(hat)

    builder
        .setTrim(ServoTrim.TOWERPRO_SG90)
        .build()
}

private val rotator by lazy {
    ServoRotator(servo, (0..180), (0..180), 2)
}

private val stopList = listOf(0, 60, 120, 180)
private val gamepad by lazy { GamepadQT() }

const val REMOTE_PI = "diozero.remote.hostname"
const val PSYCHE = "psyche.local"
const val TOPIC = "kobots/rotoMatic"
var stopIndex = 0

fun main() {
//    System.setProperty(REMOTE_PI, PSYCHE)

    rotator.swing(stopList[stopIndex])

    val mqttClient = KobotsMQTT("Rotomatic", BROKER).apply {
        startAliveCheck()
        subscribe(TOPIC) { payload ->
            if (payload.equals("next", true)) {
                rotator.next()
            } else {
                val whichOne = payload.toInt()
                if (whichOne >= 0 && whichOne < stopList.size) {
                    rotator.swing(stopList[whichOne])
                    stopIndex = whichOne
                }
            }
        }
    }

    gamepad.use { pad ->
        var done = false
        while (!done) {
            pad.read().apply {
                when {
                    a -> rotator.next()

                    y -> rotator.prev()

                    x -> {
                        val fl = servo.angle + 2f
                        servo at fl
                    }

                    b -> {
                        val fl = servo.angle - 2f
                        servo at fl
                    }

                    select -> println("Current angle ${rotator.current()}")
                    start -> done = true
                    else -> {}
                }
            }
            KobotSleep.millis(50)
        }
    }
    println("Rotomatic exit")
    exitProcess(0)
}

private fun ServoRotator.next() {
    stopIndex = (stopIndex + 1) % stopList.size
    rotator.swing(stopList[stopIndex])
}

private fun ServoRotator.prev() {
    stopIndex--
    if (stopIndex < 0) stopIndex = stopList.size - 1
    rotator.swing(stopList[stopIndex])
}

@Synchronized
private fun ServoRotator.swing(target: Int) {
    while (!rotateTo(target)) {
        KobotSleep.millis(50)
    }
}
