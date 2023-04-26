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

package crackers.kobots.buttonboard

import com.diozero.util.SleepUtil
import crackers.kobots.devices.lighting.NeoKey
import crackers.kobots.utilities.GOLDENROD
import java.awt.Color
import java.time.Duration

// TODO temporary while testing
const val REMOTE_PI = "diozero.remote.hostname"
const val USELESS = "useless.local"

val keyboard by lazy {
    NeoKey().apply { pixels.brightness = 0.05f }
}

// to prevent "double-reads" of a button while pressing
val BOUNCE_DELAY = Duration.ofMillis(50).toNanos()

// just to keep from spinning in a stupid tight loop
val LOOP_DELAY = Duration.ofMillis(50).toNanos()

/**
 * Uses NeoKey 1x4 as a HomeAssistant controller (and likely other things).
 */
fun main() {
    System.setProperty(REMOTE_PI, USELESS)
    Screen.startupSequence()

    var running = true
    while (running) {
        /*
         * This is purely button driven, so use the buttons
         */
        val buttonsPressed = whichButtons()
        val currentMenu = Menu.execute(buttonsPressed)
        if (currentMenu.isEmpty() && buttonsPressed.contains(3)) {
            running = false
        } else {
            Screen.execute(buttonsPressed, currentMenu)
            if (buttonsPressed.isNotEmpty()) {
                SleepUtil.busySleep(BOUNCE_DELAY)
            }
        }

        SleepUtil.busySleep(LOOP_DELAY)
    }
    keyboard[3] = GOLDENROD

    Screen.close()
    keyboard.close()
}

/**
 * Buttons pressed on the NeoKey
 */
private fun whichButtons(): List<Int> {
    val list = mutableListOf<Int>()
    for (i in 0..3) {
        if (keyboard[i]) {
            keyboard[i] = Color.YELLOW
            list += i
        } else {
            keyboard[i] = Color.BLACK
        }
    }
    return list
}
