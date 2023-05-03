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

import com.diozero.util.SleepUtil
import crackers.kobots.devices.expander.CRICKITHatDeviceFactory
import java.time.Duration

// devices
internal val crickitHat by lazy { CRICKITHatDeviceFactory() }

private val buttons by lazy {
    (1..4).toList().map { crickitHat.touchDigitalIn(it) }
}

private val NO_BUTTONS = listOf(false, false, false, false)
private var lastButtonValues = NO_BUTTONS

// because we're looking for "presses", only return values when a value transitions _to_ true
private fun buttonCheck(): List<Boolean> {
    val currentButtons = buttons.map { it.value }
    // TODO add an elapsed time check?
    if (currentButtons == lastButtonValues) return NO_BUTTONS
    lastButtonValues = currentButtons
    return currentButtons
}

const val WAIT_LOOP = 10L

// TODO debugging only!!!!!!
const val REMOTE_PI = "diozero.remote.hostname"
const val MARVIN = "marvin.local"

/**
 * Run this.
 */
fun main() {
    // TODO remove this
//    System.setProperty(REMOTE_PI, MARVIN)

    crickitHat.use { hat ->
        // main loop!!!!!
        lateinit var currentButtons: List<Boolean>

        while (buttonCheck().let {
                currentButtons = it
                currentButtons.isEmpty() || !currentButtons[3]
            }
        ) {
            val loopStartedAt = System.currentTimeMillis()

            val doThisStuff = ControlThing.execute(currentButtons)

            // maybe move something
            TheArm.execute(doThisStuff.armRequest)

            // do these last because they take the most time?
            Stripper.execute()

            // adjust this dynamically?
            val runTime = System.currentTimeMillis() - loopStartedAt
            val waitFor = if (runTime > WAIT_LOOP) {
                0L
            } else {
                WAIT_LOOP - runTime
            }

            SleepUtil.busySleep(Duration.ofMillis(waitFor).toNanos())
        }

        ControlThing.close()
        TheArm.close()
    }
}
