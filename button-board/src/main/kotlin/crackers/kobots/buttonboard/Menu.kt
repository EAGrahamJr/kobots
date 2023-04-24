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

import java.util.concurrent.atomic.AtomicReference

/**
 * describes the various menus available
 */
object Menu {
    val startupMenu = listOf("Rooms", "Scenes", "", "Exit")
    val firstRoomsMenu = listOf("Bedroom", "Office", "...", "Return")
    val secondRoomsMenu = listOf("Kitchen", "Living Room", "", "Return")

    val currentMenu = AtomicReference(startupMenu)

    /**
     * Evaluates the button vs which menu is showing - returns `false` when exiting
     */
    fun execute(keyIndex: Int): Boolean {
        when (currentMenu.get()) {
            startupMenu -> {
                if (keyIndex == 3) return true
                if (keyIndex == 0) {
                    currentMenu.set(firstRoomsMenu)
                } else if (keyIndex == 1) currentMenu.set(secondRoomsMenu)
            }

            firstRoomsMenu -> firstRoom(keyIndex)
            secondRoomsMenu -> secondRoom(keyIndex)
        }
        return false
    }

    fun firstRoom(keyIndex: Int) {
        when (keyIndex) {
            0 -> {}
            1 -> {}
            2 -> currentMenu.set(secondRoomsMenu)
            3 -> currentMenu.set(startupMenu)
        }
    }

    fun secondRoom(keyIndex: Int) {
        when (keyIndex) {
            0 -> {}
            1 -> {}
            2 -> {
                // do nothing
            }

            3 -> currentMenu.set(firstRoomsMenu)
        }
    }
}
