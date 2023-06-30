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

import crackers.kobots.app.arm.*
import kotlin.system.exitProcess

private val buttons by lazy { (1..4).toList().map { crickitHat.touchDigitalIn(it) } }

private val NO_BUTTONS = listOf(false, false, false, false)
private var lastButtonValues = NO_BUTTONS
private lateinit var currentButtons: List<Boolean>

// because we're looking for "presses", only return values when a value transitions _to_ true
private fun buttonCheck(manualMode: Boolean): Boolean {
    currentButtons = buttons.map { it.value }.let { read ->
        // "auto repeat" for manual mode
        if (manualMode && (read[1] || read[2])) listOf(false, read[1], read[2], false)
        else {
            if (read == lastButtonValues) {
                NO_BUTTONS
            } else {
                lastButtonValues = read
                read
            }
        }
    }
    return currentButtons.isEmpty() || !currentButtons[3] || manualMode
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

    SensorSuite.start()
    ArmMonitor.start()

    // emergency stop
    joinTopic(SensorSuite.ALARM_TOPIC, KobotsSubscriber {
        publishToTopic(TheArm.REQUEST_TOPIC, allStop)
        runFlag.set(false)
    })

    crickitHat.use { hat ->
        TheArm.start()

        // main loop!!!!!
        var manualMode = false
        while (buttonCheck(manualMode)) {
            try {
                executeWithMinTime(WAIT_LOOP) {
                    // figure out if we're doing anything
                    if (currentButtons.isNotEmpty())
                        manualMode = if (manualMode) manualMode() else demoMode()
                }
            } catch (e: Exception) {
                println("Exception: $e")
            }
        }
        runFlag.set(false)
        TheArm.stop()
    }
    SensorSuite.close()
    ArmMonitor.stop()
    executor.shutdownNow()
    exitProcess(0)
}

private fun demoMode(): Boolean {
    when {
        currentButtons[0] -> publishToTopic(TheArm.REQUEST_TOPIC, tireDance)
        currentButtons[1] -> return true
        currentButtons[2] -> publishToTopic(TheArm.REQUEST_TOPIC, sayHi)
    }
    return false
}

private fun manualMode(): Boolean {
    when {
        currentButtons[0] -> publishToTopic(TheArm.REQUEST_TOPIC, ManualMode())
        currentButtons[1] -> publishToTopic(TheArm.REQUEST_TOPIC, ManualMode(false))
        currentButtons[2] -> publishToTopic(TheArm.REQUEST_TOPIC, ManualMode(true))
        // bail
        currentButtons[3] -> {
            publishToTopic(TheArm.STATE_TOPIC, ManualModeEvent(-1))
            return false
        }
    }
    return true
}
