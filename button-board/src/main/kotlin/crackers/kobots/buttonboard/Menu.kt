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

/**
 * describes the various menus available
 */
object Menu {
    val startupMenu = listOf("Scene 1", "Scene 2", "...", "Exit")

    //    val secondMenu = listOf("TV", "After TV", "...", "Return")
    val secondMenu = listOf("Bedroom On", "Bedroom Off", "...", "Return")
    val thirdMenu = listOf("Not Bedroom", "Movie Time", "", "Return")

    private var current = emptyList<String>()
    private val hasskClient: HAssKClient

    init {
        val config = ConfigFactory.load()
        hasskClient = HAssKClient(
            config.getString("ha.token"),
            config.getString("ha.server"),
            config.getInt("ha.port")
        )
    }

    /**
     * Evaluates the button vs which menu is showing
     */
    fun execute(keysPressed: List<Int>): List<String> {
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
                0 -> {}//scene("top_button") turn on
                1 -> {}//scene("bed_time") turn on
                2 -> current = secondMenu
                else -> current = emptyList()

            }
        }
    }

    private fun executeSecondMenu(keyIndex: Int) {
        with(hasskClient) {
            when (keyIndex) {
                0 -> light("bedroom_group") turn on //scene("daytime_tv") turn on
                1 -> light("bedroom_group") turn off //scene("post_tv") turn on
                2 -> current = thirdMenu
                else -> current = startupMenu
            }
        }
    }

    private fun executeThirdMenu(keyIndex: Int) {
        with(hasskClient) {
            when (keyIndex) {
                0 -> {}//light("not_bedroom_group") turn off
                1 -> {}//scene("movie_time") turn on
                2 -> {
                    // do nothing
                }

                else -> current = secondMenu
            }
        }
    }
}
