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
import com.diozero.devices.ServoController
import crackers.kobots.devices.at
import crackers.kobots.devices.io.GamepadQT
import crackers.kobots.mqtt.KobotsMQTT
import crackers.kobots.parts.ServoRotator
import crackers.kobots.utilities.KobotSleep
import org.json.JSONObject
import kotlin.system.exitProcess

/**
 * Handles a bunch of different servos for various things.
 *
 * - RotoMatic uses a Rotator to select a thing. Manual selections are through something with buttons. Also has an MQTT
 *  interface.
 * - FlagNag is a servo that moves a flag back and forth to get attention
 */

lateinit var rotoServo: ServoDevice
val rotator by lazy { ServoRotator(rotoServo, (0..360), (0..180)) }
val stopList = listOf(0, 58, 115, 184, 254, 312)

lateinit var flagServo: ServoDevice
val nagger by lazy { ServoRotator(flagServo, (0..180), (0..180)) }

const val REMOTE_PI = "diozero.remote.hostname"
const val PSYCHE = "psyche.local"
const val RQST_TOPIC = "kobots/rotoMatic"
const val EVENT_TOPIC = "kobots/events"
var stopIndex = 0

val mqttClient = KobotsMQTT("Rotomatic", "tcp://192.168.1.4:1883").apply {
    startAliveCheck()
    subscribe(RQST_TOPIC) { payload ->
        when {
            payload.equals("stop", true) -> done = true
            payload.equals("next", true) -> rotator.next()
            payload.equals("prev", true) -> rotator.prev()
            payload.equals("nag", true) -> flagServo.nag()
            else -> {
                val whichOne = payload.toInt()
                if (whichOne >= 0 && whichOne < stopList.size) {
                    rotator.swing(stopList[whichOne])
                    stopIndex = whichOne
                    publish(
                        EVENT_TOPIC,
                        JSONObject().apply {
                            put("source", "rotomatic")
                            put("selected", stopIndex)
                        }.toString()
                    )
                }
            }
        }
    }
}

@Volatile
var done: Boolean = false

fun main() {
    SensorSuite.start()
//    System.setProperty(REMOTE_PI, PSYCHE)
    ServoController().use { hat ->
        rotoServo = hat.getServo(0, ServoTrim.TOWERPRO_SG90, 0)
        flagServo = hat.getServo(1, ServoTrim(1500, 1100), 0)

        rotator.swing(stopList[stopIndex])
        nagger.swing(90, 15)

        GamepadQT().use { pad ->
            nagger.swing(0, 15)
            runLoop(pad)
        }
        println("Rotomatic exit")
        rotator.swing(0)
        flagServo at 0
    }
    SensorSuite.close()
    exitProcess(0)
}

private fun runLoop(pad: GamepadQT) {
    var manualServo = rotoServo

    while (!done) {
        pad.read().apply {
            when {
                a -> rotator.next()
                y -> rotator.prev()

                x -> {
                    val fl = manualServo.angle + 1f
                    manualServo at fl
                }

                b -> {
                    val fl = manualServo.angle - 1f
                    manualServo at fl
                }

                select -> {
//                    println("Current: rotator ${rotator.current()}, servo ${rotoServo.angle}")
                    if (manualServo == rotoServo) {
                        manualServo = flagServo
                        println("Flag servo")
                    } else {
                        manualServo = rotoServo
                        println("Roto servo")
                    }
                }

                start -> done = true
                else -> {}
            }
        }
        KobotSleep.millis(50)
    }
}

@Synchronized
fun ServoRotator.next() {
    stopIndex = (stopIndex + 1) % stopList.size
    rotator.swing(stopList[stopIndex])
}

@Synchronized
fun ServoRotator.prev() {
    stopIndex--
    if (stopIndex < 0) stopIndex = stopList.size - 1
    rotator.swing(stopList[stopIndex])
}

@Synchronized
fun ServoRotator.swing(target: Int, speed: Long = 75) {
    while (!rotateTo(target)) {
        KobotSleep.millis(speed)
    }
}

@Synchronized
fun ServoDevice.home() {
    var a = angle
    while (a != 0f) {
        at(a - 1f)
        KobotSleep.millis(20)
        a = angle
    }
}

@Synchronized
fun ServoDevice.nag() {
    nagger.swing(90, 50)

    for (repeat in 1..5) {
        for (angle in 60..120 step 5) {
            KobotSleep.millis(30)
            at(angle)
        }
    }
    nagger.swing(0, 50)
}
