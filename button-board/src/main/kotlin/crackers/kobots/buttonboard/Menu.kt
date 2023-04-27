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

import com.typesafe.config.ConfigFactory
import crackers.hassk.Constants.off
import crackers.hassk.Constants.on
import crackers.hassk.HAssKClient
import crackers.kobots.buttonboard.Menu.ItemType.*

/**
 * describes the various menus available
 */
internal object Menu {
    internal enum class ItemType {
        NOOP, ACTION, NEXT, PREV, EXIT
    }

    internal class MenuItem(val name: String, val type: ItemType = ACTION)

    private val NEXT_ITEM = MenuItem("...", NEXT)
    private val PREV_ITEM = MenuItem("Return", PREV)

    private val startupMenu = listOf(
        MenuItem("Morning"),
        MenuItem("Daytime"),
        NEXT_ITEM,
        MenuItem("Exit", EXIT)
    )
    private val secondMenu = listOf(
        MenuItem("Bed Time"),
        MenuItem("Not All"),
        NEXT_ITEM,
        PREV_ITEM
    )
    private val thirdMenu = listOf(
        MenuItem("TV"),
        MenuItem("Movie Time"),
        MenuItem("", NOOP),
        PREV_ITEM
    )

    private var current = emptyList<MenuItem>()
    private val hasskClient = with(ConfigFactory.load()) {
        HAssKClient(getString("ha.token"), getString("ha.server"), getInt("ha.port"))
    }

    /**
     * Evaluates the button vs which menu is showing
     */
    internal fun execute(keysPressed: List<Int>): List<MenuItem> {
        if (keysPressed.isNotEmpty()) {
            val keyIndex = keysPressed[0]
            when (current) {
                startupMenu -> executeStartupMenu(keyIndex)
                secondMenu -> executeSecondMenu(keyIndex)
                thirdMenu -> executeThirdMenu(keyIndex)
                else -> current = startupMenu
            }
        }
        return current
    }

    private fun executeStartupMenu(keyIndex: Int) {
        with(hasskClient) {
            when (keyIndex) {
                0 -> scene("early_morning") turn on
                1 -> scene("top_button") turn on
                2 -> current = secondMenu
                else -> current = emptyList()
            }
        }
    }

    private fun executeSecondMenu(keyIndex: Int) {
        with(hasskClient) {
            when (keyIndex) {
                0 -> scene("bed_time") turn on
                1 -> light("not_bedroom_group") turn off
                2 -> current = thirdMenu
                else -> current = startupMenu
            }
        }
    }

    private fun executeThirdMenu(keyIndex: Int) {
        with(hasskClient) {
            when (keyIndex) {
                0 -> scene("daytime_tv") turn on // scene("daytime_tv") turn on
                1 -> scene("movie_time") turn on
                2 -> {
                    // do nothing
                }

                else -> current = secondMenu
            }
        }
    }
}
