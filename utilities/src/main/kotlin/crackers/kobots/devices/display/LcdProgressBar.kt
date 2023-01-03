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

package crackers.kobots.devices.display

import com.diozero.devices.LcdInterface
import kotlin.math.roundToInt

/**
 * Creates a "progress bar" that occupies one line of an LCD. The default line is `0`, or the top-most.
 *
 * The bar uses `|`,`||`, `|||` and `|||!` custom characters (effectively) to fill each character cell. The bar may
 * be shown either left-to-right (default) or right-to-left.
 */
class LcdProgressBar(
    private val lcd: LcdInterface<*>,
    private val displayRow: Int = 0,
    private val leftToRight: Boolean = true
) {
    private val charbars: List<Int>
    private val percentPerColumn = lcd.columnCount / 100f
    private val rightMostColumn = lcd.columnCount - 1

    init {
        val whichWay = if (leftToRight) "asc" else "desc"
        charbars = (0..3).map {
            val location = it + 2
            lcd.createChar(location, LcdInterface.Characters.get("${whichWay}progress${it + 1}"))
            location
        }
    }


    /**
     * Getting/setting for value - percentage 0 to 100
     */
    private var latest: Int = 0
    var value: Int
        get() = latest
        set(value) {
            if (value in 0..100) {
                latest = value
                val columns = value * percentPerColumn
                val cells = columns.toInt()
                val bars = ((columns - cells) * 4).roundToInt()

                (0..cells).forEach {
                    if (cells <= rightMostColumn) {
                        val offset = if (leftToRight) cells else rightMostColumn - cells

                        lcd.setCursorPosition(offset, displayRow)
                        lcd.addText(charbars[bars])
                    }
                }
            }
        }
}
