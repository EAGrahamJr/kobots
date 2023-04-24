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
import java.time.Instant
import java.util.*

// TODO temporary while testing
const val REMOTE_PI = "diozero.remote.hostname"
const val USELESS = "useless.local"

val keyboard by lazy {
    NeoKey().apply { pixels.brightness = 0.05f }
}

private val BUTTON_DELAY = Duration.ofSeconds(2)
private lateinit var buttonsPressed: List<Int>
private var lastButtonTime = Instant.now().minus(BUTTON_DELAY)

/**
 * Uses NeoKey 1x4 as a HomeAssistant controller (and likely other things).
 */
fun main() {
    System.setProperty(REMOTE_PI, USELESS)
    Screen.startupSequence()

    keyboard[3] = Color.GREEN
    var die = false
    while (!die) {
        val buttonsPressed = whichButtons()
        if (buttonsPressed.isPresent()) {
            val button = buttonsPressed.get()

            // if the screen is on, interactive mode
            // otherwise wake up or die
            if (Screen.isOn()) {
                die = Menu.execute(button)
            } else {
                keyboard[3] = Color.RED
                die = button == 3
                if (!die) Screen.enableDisplay()
            }
        }
        Screen.execute()
        SleepUtil.busySleep(Duration.ofMillis(100).toNanos())
    }
    keyboard[3] = GOLDENROD

    Screen.close()
    keyboard.close()
}

/**
 * First button pressed
 */
private fun whichButtons(): Optional<Int> {
    if (Duration.between(lastButtonTime, Instant.now()).compareTo(BUTTON_DELAY) > 0) {
        for (i in 0..3) {
            if (keyboard[i]) {
                keyboard[i] = Color.YELLOW
                lastButtonTime = Instant.now()
                return Optional.of(i)
            }
        }
    }
    return Optional.empty()
}
