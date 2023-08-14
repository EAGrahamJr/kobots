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

import com.diozero.devices.ServoController
import crackers.kobots.devices.at
import crackers.kobots.devices.io.GamepadQT
import crackers.kobots.mqtt.KobotsMQTT
import crackers.kobots.mqtt.KobotsMQTT.Companion.BROKER
import crackers.kobots.parts.ServoRotator
import crackers.kobots.utilities.KobotSleep
import org.json.JSONObject
import kotlin.system.exitProcess

/**
 * Handles a bunch of different servos for various things.
 *
 * - RotoMatic uses a Rotator to select a thing. Manual selections are through something with buttons. Also has an MQTT
 *  interface.
 */

private val hat = ServoController()

// TODO restore servo trim when it works
private val rotoServo = hat.getServo(0).apply { angle = 5f }
private val rotator = ServoRotator(rotoServo, (0..360), (0..180))

private val stopList = listOf(10, 76, 132, 212, 284)
private val gamepad by lazy { GamepadQT() }

const val REMOTE_PI = "diozero.remote.hostname"
const val PSYCHE = "psyche.local"
const val TOPIC = "kobots/rotoMatic"
const val EVENT_TOPIC = "kobots/events"
var stopIndex = 0

fun main() {
//    System.setProperty(REMOTE_PI, PSYCHE)

    rotator.swing(stopList[stopIndex])

    val mqttClient = KobotsMQTT("Rotomatic", BROKER).apply {
        startAliveCheck()
        subscribe(TOPIC) { payload ->
            when {
                payload.equals("next", true) -> rotator.next()
                payload.equals("prev", true) -> rotator.prev()
                else -> {
                    val whichOne = payload.toInt()
                    if (whichOne >= 0 && whichOne < stopList.size) {
                        rotator.swing(stopList[whichOne])
                        stopIndex = whichOne
                        publish(EVENT_TOPIC, JSONObject().apply {
                            put("source", "rotomatic")
                            put("selected", stopIndex)
                        }.toString())
                    }
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
                        val fl = rotoServo.angle + 1f
                        rotoServo at fl
                    }

                    b -> {
                        val fl = rotoServo.angle - 1f
                        rotoServo at fl
                    }

                    select -> println("Current: rotator ${rotator.current()}, servo ${rotoServo.angle}")
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
