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

import crackers.kobots.app.arm.ArmMonitor
import crackers.kobots.app.arm.TheArm
import crackers.kobots.app.arm.armSequence
import kotlin.system.exitProcess

private val buttons by lazy { (1..4).toList().map { crickitHat.touchDigitalIn(it) } }

private val NO_BUTTONS = listOf(false, false, false, false)
private var lastButtonValues = NO_BUTTONS
private lateinit var currentButtons: List<Boolean>

// because we're looking for "presses", only return values when a value transitions _to_ true
private fun buttonCheck(): Boolean {
    currentButtons = buttons.map { it.value }.let { read ->
        // TODO add an elapsed time check?

        if (read == lastButtonValues) {
            NO_BUTTONS
        } else {
            lastButtonValues = read
            read
        }
    }
    return currentButtons.isEmpty() || !currentButtons[3]
}

private const val WAIT_LOOP = 100L

// TODO temporary while testing
const val REMOTE_PI = "diozero.remote.hostname"
const val MARVIN = "marvin.local"

/**
 * Run this.
 */
fun main() {
//    System.setProperty(REMOTE_PI, MARVIN)

    ProximitySensor.start()
    ArmMonitor.start()

    // emergency stop
    joinTopic(ProximitySensor.ALARM_TOPIC, KobotsSubscriber {
        publishToTopic(TheArm.REQUEST_TOPIC, allStop)
        runFlag.set(false)
    })

    crickitHat.use { hat ->
        TheArm.start()
        val xAxis = hat.signalAnalogIn(6)
        val yAxis = hat.signalAnalogIn(7)
        val zButton = hat.signalDigitalIn(8)
        val waist = hat.signalAnalogIn(1)

        // main loop!!!!!
        var ignoreJoystick = true
        while (buttonCheck()) {
            try {
                executeWithMinTime(WAIT_LOOP) {
                    // figure out if we're doing anything
                    when {
                        currentButtons[0] -> publishToTopic(TheArm.REQUEST_TOPIC, tireDance)
                        currentButtons[1] -> {
//                        publishToTopic(TheArm.REQUEST_TOPIC, downAndOut)
                            ignoreJoystick = !ignoreJoystick
//                        if (!ignoreJoystick) publishToTopic(TheArm.REQUEST_TOPIC, armPark)
                        }

//                    currentButtons[2] -> publishToTopic(TheArm.REQUEST_TOPIC, armPark)
                    }

                    if (!ignoreJoystick) {
                        publishToTopic(TheArm.REQUEST_TOPIC, armSequence {
                            movement {
                                waist { angle = (waist.unscaledValue * 180) }
                                shoulder { angle = (xAxis.unscaledValue * 180) }
                                elbow { angle = (yAxis.unscaledValue * 180) }
                                if (zButton.value) gripperOpen() else gripperClose()
                            }
                        })
                    }
                }
            } catch (e: Exception) {
                println("Exception: $e")
            }
        }
        runFlag.set(false)
        TheArm.stop()
    }
    ProximitySensor.close()
    ArmMonitor.stop()
    executor.shutdownNow()
    exitProcess(0)
}
