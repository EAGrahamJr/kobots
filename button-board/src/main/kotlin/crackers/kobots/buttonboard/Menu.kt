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

/**
 * describes the various menus available
 */
object Menu {
    val startupMenu = listOf("Rooms", "Scenes", "", "Exit")
    val firstRoomsMenu = listOf("Bedroom", "Office", "...", "Return")
    val secondRoomsMenu = listOf("Kitchen", "Living Room", "", "Return")

    private var current = emptyList<String>()

    /**
     * Evaluates the button vs which menu is showing - returns an empty list when "exit" is selected.
     */
    fun execute(keysPressed: List<Int>): List<String> {
        if (keysPressed.isNotEmpty()) {
            val keyIndex = keysPressed[0]
            when (current) {
                startupMenu -> {
                    if (keyIndex == 3) return emptyList()
                    if (keyIndex == 0) current = firstRoomsMenu else if (keyIndex == 1) current = secondRoomsMenu
                }

                firstRoomsMenu -> firstRoom(keyIndex)
                secondRoomsMenu -> secondRoom(keyIndex)
                else -> current = startupMenu
            }
        }
        return current
    }

    fun firstRoom(keyIndex: Int) {
        when (keyIndex) {
            0 -> {}
            1 -> {}
            2 -> current = secondRoomsMenu
            3 -> current = startupMenu
        }
    }

    fun secondRoom(keyIndex: Int) {
        when (keyIndex) {
            0 -> {}
            1 -> {}
            2 -> {
                // do nothing
            }

            3 -> current = firstRoomsMenu
        }
    }
}
