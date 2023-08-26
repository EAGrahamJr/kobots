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

package crackers.kobots

import crackers.kobots.utilities.center
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D

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

/**
 * TODO can't believe I'm doing this...
 */
const val REMOTE_PI = "diozero.remote.hostname"

interface ColumnDisplay {
    fun Graphics2D.clearImage()
    fun Graphics2D.displayStatuses(status: Map<String, Number>)
}

class DelgatedColumnDisplay(private val widthOfDisplay: Int, private val heightOfDisplay: Int) : ColumnDisplay {
    override fun Graphics2D.clearImage() {
        color = Color.BLACK
        fillRect(0, 0, widthOfDisplay, heightOfDisplay)
    }

    override fun Graphics2D.displayStatuses(mapped: Map<String, Number>) {
        val colHeaders = mapped.keys
        val columnWidth = (widthOfDisplay / colHeaders.size)
        val halfHeightOfDisplay = heightOfDisplay / 2
        val ogFont = font
        val headerFont = font.deriveFont(Font.BOLD)

        clearImage()
        colHeaders.forEachIndexed { i, header ->
            val colPosition = i * columnWidth
            val drawWidth = columnWidth - 1

            // position headers
            color = Color.GRAY
            fillRect(colPosition, 0, drawWidth, halfHeightOfDisplay)
            color = Color.BLACK
            font = headerFont
            val headerX = colPosition + fontMetrics.center(header, drawWidth)
            drawString(header, headerX, fontMetrics.ascent)

            // position values
            color = Color.WHITE
            font = ogFont
            val value = mapped[header].toString()
            val valueX = colPosition + fontMetrics.center(value, drawWidth)
            drawString(value, valueX, heightOfDisplay - 1)
        }
    }
}
