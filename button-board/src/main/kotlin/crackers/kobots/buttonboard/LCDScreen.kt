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

import com.diozero.devices.HD44780Lcd
import com.diozero.devices.LcdConnection.PCF8574LcdConnection
import com.diozero.devices.LcdInterface.Characters
import crackers.kobots.devices.display.LcdProgressBar
import java.lang.Thread.sleep
import java.time.Duration
import java.time.Instant

/**
 * Two-line LCD screen to show the menus.
 */
internal object LCDScreen : BBScreen {
    private val screen = HD44780Lcd(PCF8574LcdConnection(1), 16, 2)
    private var lastDisplayed = Instant.now()
    private var lastMenu: List<String>? = null

    private const val MAX_TIME = 30

    override fun close() = screen.close()

    override var on: Boolean
        get() = screen.isBacklightEnabled
        set(b) {
            screen.setBacklightEnabled(b)
        }

    override fun startupSequence() {
        with(screen) {
            setBacklightEnabled(true)
            setText(0, "System start...")
            createChar(0, Characters.get("arrowright") + 0x00)
            createChar(1, Characters.get("retarrow") + 0x00)
            LcdProgressBar(screen, 1).apply {
                (0..100).forEach {
                    value = it
                    sleep(15)
                }
            }
            setText(1, "Ready...                            ")
            sleep(15)
        }
        lastDisplayed = Instant.now()
    }

    override fun execute(buttonsPressed: Boolean, currentMenu: List<Menu.MenuItem>) {
        val now = Instant.now()

        if (buttonsPressed) {
            lastDisplayed = now
            on = true
            lastMenu = null
        }

        // check the timer - turn the screen off if on too long
        if (on) {
            Duration.between(lastDisplayed, now).toSeconds().toInt().also { elapsed ->
                if (elapsed >= MAX_TIME) {
                    on = false
                } else {
                    showMenu(currentMenu.map { it.name })
                }
            }
        }
    }

    private fun showMenu(menu: List<String>) {
        if (menu.isEmpty() || menu == lastMenu) return
        lastMenu = menu

        // each item gets 8 spaces
        val menuBuffer = StringBuffer(formatMenuItem(0, menu))
            .append(formatMenuItem(2, menu))
            .append('\n')
            .append(formatMenuItem(1, menu))
            .append(formatMenuItem(3, menu))
            .toString()
        screen.displayText(menuBuffer)
    }

    private fun formatMenuItem(index: Int, currentMenu: List<String>): String {
        val item = currentMenu[index].trimEnd()
        return if (item.isEmpty()) {
            "        "
        } else {
            val padded = item.padEnd(6).let {
                if (it.length > 6) it.substring(0, 6) else it
            }
            String.format("%d-%-6s ", index + 1, padded)
        }
    }
}
