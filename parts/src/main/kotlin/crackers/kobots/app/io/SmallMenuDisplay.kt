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

import crackers.kobots.utilities.center
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage

/**
 * Creates screen graphics for the NeoKeyMenu display. This is primarily intended for use with a small OLED display
 * (e.g. 128x32). This class only creates the graphics portion of an image. Actual display is handled elsewhere.
 *
 * Note: text is **NOT** truncated, so if the text is "too long", it may overwrite other text.
 */
abstract class SmallMenuDisplay(private val mode: DisplayMode = DisplayMode.TEXT) : NeoKeyMenu.MenuDisplay {

    enum class DisplayMode {
        ICONS, TEXT
    }

    private val menuGraphics: Graphics2D

    private val withImagestextHeight: Int
    private val withImagestextBaseline: Int
    private val iconImageSize: Int
    private val withImagesFont: Font = Font(Font.SANS_SERIF, Font.PLAIN, 9)

    private val withTextFont = Font(Font.SANS_SERIF, Font.PLAIN, 12)
    private var withTextFirstLine: Int
    private val withTextSecondLine: Int

    protected val menuImage = BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_BYTE_GRAY).also { img ->
        menuGraphics = (img.graphics as Graphics2D).apply {
            val fm = getFontMetrics(withImagesFont)
            withImagestextHeight = fm.height
            withImagestextBaseline = IMG_HEIGHT - fm.descent
            iconImageSize = IMG_HEIGHT - withImagestextHeight

            withTextFirstLine = getFontMetrics(withTextFont).ascent + 1
            withTextSecondLine = IMG_HEIGHT - getFontMetrics(withTextFont).descent
        }
    }

    protected fun showIcons(items: List<NeoKeyMenu.MenuItem>) {
        with(menuGraphics) {
            font = withImagesFont
            // clear the image
            color = Color.BLACK
            fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT)
            // draw and scale the icons
            color = Color.WHITE
            items.forEachIndexed { index, item ->
                val offset = index * IMG_HEIGHT
                val imageX = offset + withImagestextHeight / 2
                if (item.icon != null) {
                    drawImage(item.icon, imageX, 0, iconImageSize, iconImageSize, null)
                }
                val text = item.toString()
                val textX = offset + fontMetrics.center(text, IMG_HEIGHT)
                drawString(text, textX, withImagestextBaseline)
            }
        }
    }

    protected fun showText(items: List<NeoKeyMenu.MenuItem>) {
        val secondColumnOffset = (MAX_WD / 2) - 3
        with(menuGraphics) {
            font = withTextFont
            // clear the image
            color = Color.BLACK
            fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT)
            // draw the text
            color = Color.WHITE
            drawString(items[0].toString(), 0, withTextFirstLine)
            drawString(items[1].toString(), secondColumnOffset, withTextFirstLine)
            drawString(items[2].toString(), 0, withTextSecondLine)
            drawString(items[3].toString(), secondColumnOffset, withTextSecondLine)
        }
    }

    override fun displayItems(items: List<NeoKeyMenu.MenuItem>) {
        when (mode) {
            DisplayMode.ICONS -> showIcons(items)
            DisplayMode.TEXT -> showText(items)
        }
        drawMenuImage(menuImage)
    }

    abstract protected fun drawMenuImage(menuImage: BufferedImage)

    companion object {
        const val IMG_WIDTH = 128
        const val IMG_HEIGHT = 32
        private const val HALF_HT = IMG_HEIGHT / 2
        private const val MAX_WD = IMG_WIDTH - 1
    }
}
