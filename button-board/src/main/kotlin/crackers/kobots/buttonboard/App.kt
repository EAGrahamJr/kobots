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
import crackers.kobots.utilities.PURPLE
import org.slf4j.LoggerFactory
import java.awt.Color
import java.time.Duration
import java.time.LocalTime

private val keyboard by lazy {
    NeoKey().apply { pixels.brightness = 0.05f }
}

// just to keep from spinning in a stupid tight loop
private val ACTIVE_DELAY = Duration.ofMillis(50).toNanos()
private val SLEEP_DELAY = Duration.ofSeconds(1).toNanos()

// TODO temporary while testing
const val REMOTE_PI = "diozero.remote.hostname"
const val USELESS = "useless.local"

/**
 * Uses NeoKey 1x4 as a HomeAssistant controller (and likely other things).
 */
fun main() {
//    System.setProperty(REMOTE_PI, USELESS)
    val screen: BBScreen = SmallScreen
    screen.startupSequence()

    var running = true
    keyboard[3] = Color.RED
    var lastButtonsRead: List<Boolean> = listOf(false, false, false, false)
    var currentMenu: List<Menu.MenuItem> = emptyList()

    var lastCurrent: List<Menu.MenuItem>? = null

    while (running) {
        try {
            brightness() // adjust per time of day
            /*
             * This is purely button driven, so use the buttons - try to "debounce" by only detecting changes between
             * iterations. This is because humans are slow
             */
            val currentButtons = keyboard.read()
            var whichButtonsPressed: List<Int> = emptyList()

            // no change, don't anything, otherwise figure out what was asked for
            if (currentButtons == lastButtonsRead) {
                if (!screen.on) buttonsWait()
            } else {
                lastButtonsRead = currentButtons
                whichButtonsPressed = currentButtons.mapIndexedNotNull { index, b ->
                    if (b) {
                        keyboard[index] = Color.YELLOW
                        index
                    } else {
                        null
                    }
                }

                // only do anything if the screen is on
                currentMenu = if (screen.on) {
                    Menu.execute(whichButtonsPressed).mapIndexed { index, item ->
                        keyboard[index] = when (item.type) {
                            Menu.ItemType.NOOP -> Color.BLACK
                            Menu.ItemType.ACTION -> Color.GREEN
                            Menu.ItemType.NEXT -> Color.CYAN
                            Menu.ItemType.PREV -> Color.BLUE
                            Menu.ItemType.EXIT -> Color.RED
                        }
                        item
                    }
                } else {
                    buttonsWait()
                    emptyList()
                }
            }

            if (currentMenu != lastCurrent) {
                lastCurrent = currentMenu
            }

            if (currentMenu.isEmpty() && whichButtonsPressed.contains(3)) {
                running = false
            } else {
                val buttonWasPressed = whichButtonsPressed.isNotEmpty()
                screen.execute(buttonWasPressed, currentMenu)
            }
            SleepUtil.busySleep(if (screen.on) ACTIVE_DELAY else SLEEP_DELAY)
        } catch (e: Throwable) {
            LoggerFactory.getLogger("ButtonBox").error("Error found - continuing", e)
        }
    }
    keyboard[3] = GOLDENROD

    screen.close()
    keyboard.close()
}

/**
 * Set "sleeping" colors on the buttons.
 */
private fun buttonsWait() {
    if ((keyboard color 0).color != PURPLE) {
        (0..2).forEach { keyboard[it] = PURPLE }
        keyboard[3] = Color.RED
    }
}

/**
 * Adjust brightness according to the hour of day.
 */
private fun brightness() {
    LocalTime.now().also { t ->
        if (t.minute == 0) {
            val b = if (t.hour >= 22 || t.hour < 8) .01f else .05f
            if (b != keyboard.pixels.brightness) keyboard.pixels.brightness = b
        }
    }
}
