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
import java.util.concurrent.atomic.AtomicInteger

/**
 * An "actionable" menu using the NeoKey. Due to the limited number of buttons, the menu is "rotated" between the
 * number of keys available. The [display] will get a subset of `n-1` items, plus a "next" item. The "next" item will
 * rotate the menu to the next subset of items.
 *
 * The calling application can use the return map to determine which button was pressed and what action to take,
 * exeucting more than one if desired. This also allows the application to use the keys to "chord" actions that are
 * not necessarily menu items.
 */
open class NeoKeyMenu(val neoKey: NeoKeyHandler, val display: MenuDisplay, items: List<MenuItem>) {
    private val logger = LoggerFactory.getLogger("NeoKeyMenu")

    // clone immutable list
    private val menuItems = items.toList()

    // take off one for the "next" button
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

    /**
     * Describes a menu item.
     */
    class MenuItem(
        val name: String,
        val abbrev: String? = null,
        val icon: Image? = null,
        val buttonColor: Color = Color.GREEN,
        val action: () -> Unit
    ) {
        override fun toString() = abbrev ?: name
    }

    private val currentMenuItem = AtomicInteger(0)

    private val nextMenuItem = MenuItem("Next", abbrev = "\u25B7", buttonColor = Color.BLUE) {
        rotateMenu(currentMenuItem.get() + maxKeys)
    }

    private fun rotateMenu(proposed: Int) {
        var nextIndex = proposed
        if (nextIndex >= menuItems.size) nextIndex = 0
        else if (nextIndex + maxKeys >= menuItems.size) nextIndex = menuItems.size - maxKeys
        currentMenuItem.set(nextIndex)
        displayMenu()
    }

    /**
     * Reads the keyboard and maps buttons pressed to actions to be performed.
     */
    @Synchronized
    open fun execute(): List<Pair<Int, MenuItem>> {
        val keys = neoKey.read()
        val result = mutableListOf<Pair<Int, MenuItem>>()
        for (i in 0..maxKeys - 1) if (keys[i]) result += i to menuItems[currentMenuItem.get() + i]
        if (keys[3]) result += 3 to nextMenuItem
        return result
    }

    /**
     * Sets up the sub-selection of menu items and displays them.
     */
    @Synchronized
    fun displayMenu() {
        val fromIndex = currentMenuItem.get()
        val toDisplay = menuItems.subList(fromIndex, fromIndex + maxKeys).toMutableList()
        toDisplay += nextMenuItem

        // display the stuff and set the button colors
        display.display(toDisplay)
        neoKey.buttonColors = toDisplay.map { it.buttonColor }
    }
}
