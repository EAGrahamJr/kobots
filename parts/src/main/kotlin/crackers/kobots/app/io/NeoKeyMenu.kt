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

package crackers.kobots.app.io

import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Image
import java.awt.MenuItem
import java.util.concurrent.atomic.AtomicInteger

/**
 * An "actionable" menu using the NeoKey. Due to the limited number of buttons, the menu is "rotated" between the
 * number of keys available. The [display] will get a subset of `n-1` items, plus a "next" item. The "next" item will
 * rotate the menu to the next subset of items.
 */
class NeoKeyMenu(val neoKey: NeoKeyHandler, val display: MenuDisplay, items: List<MenuItem>) {
    private val logger = LoggerFactory.getLogger("NeoKeyMenu")

    // clone immutable list
    private val menuItems = items.toList()
    private val maxKeys = neoKey.numberOfButtons - 1

    /**
     * Describes a menu item.
     * @param name the name of the menu item
     * @param abbrev an optional abbreviation for the menu item
     * @param icon an optional icon for the menu item
     * @param buttonColor the color to use for the button (default GREEN)
     * @param action the action to take when the button is pressed
     */

    /**
     * Displays menu items.
     */
    interface MenuDisplay {
        fun display(items: List<MenuItem>)
    }


    class MenuItem(
        val name: String,
        val abbrev: String? = null,
        val icon: Image? = null,
        val buttonColor: Color = Color.GREEN,
        val action: () -> Unit
    )

    private val currentMenuItem = AtomicInteger(0)
    val nextMenuItem = MenuItem("Next", "\u25B7", buttonColor = Color.BLUE) {
        currentMenuItem.set(currentMenuItem.incrementAndGet() % items.size)
        displayMenu()
    }

    /**
     * Reads the keyboard and does the things.
     */
    fun execute() {
        val button = neoKey.read().indexOf(true)
        if (button >= 0) {
            menuItems[currentMenuItem.get() + button].apply {
                try {
                    action()
                } catch (t: Throwable) {
                    logger.error("Error executing menu item ${name}", t)
                }
            }
        }
    }

    private fun displayMenu() {
        val fromIndex = currentMenuItem.get()
        val toDisplay = menuItems.subList(fromIndex, fromIndex + maxKeys).toMutableList()
        toDisplay += nextMenuItem

        // display the stuff and set the button colors
        display.display(toDisplay)
        neoKey.setButtonColors(toDisplay.map { it.buttonColor })
    }
}
