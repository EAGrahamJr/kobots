/*
 * Copyright 2022-2024 by E. A. Graham, Jr.
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

package crackers.kobots.app.display

import crackers.kobots.graphics.loadImage
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import kotlin.math.roundToInt

/**
 * 8-ball for small displays. Default size is for a "large" OLED (128x128).
 *
 * An image of the 8-ball is first displayed for a given period of time (default 5 seconds) and then a randomly selected
 * "fortune" is presented.
 */
class EightBall(private val graphics: Graphics2D, private val width: Int = 128, private val height: Int = 128) {
    private val eightBallFont: Font

    // TODO use multi-line responses, line-breaks, and a bigger font!
    private val eightBallList = listOf(
        "It is certain", "Reply hazy, try again", "Don’t count on it", "Don’t count on it",
        "Ask again later", "My reply is no", "Without a doubt", "Better not tell you now",
        "My sources say no", "Yes definitely", "Cannot predict now", "Outlook not so good",
        "You may rely on it", "Concentrate and ask again", "Very doubtful",
        "As I see it, yes", "Most likely", "Outlook good", "Yes", "Signs point to yes"
    )
        .also { theList ->
            var fitThis = ""
            theList.forEach { if (it.length > fitThis.length) fitThis = it }
            var fontSize = 18.0f
            var font = Font(Font.SERIF, Font.PLAIN, fontSize.roundToInt())
            while (font.size > 0f) {
                if (graphics.getFontMetrics(font).stringWidth(fitThis) < width) break
                fontSize = fontSize - .5f
                font = font.deriveFont(fontSize)
            }
            if (font.size < 1f) throw IllegalStateException("Cannot get font size")
            eightBallFont = font
        }

    private val eightBallImage by lazy { loadImage("/8-ball.png") }
    private val imageX = (width - height) / 2

    fun image() = with(graphics) {
        clearRect(0, 0, width, height)
        drawImage(eightBallImage, imageX, 0, height, height, null)
    }

    fun next() = with(graphics) {
        color = Color.WHITE
        background = Color.BLACK
        font = eightBallFont

        clearRect(0, 0, width, height)
        val sayThis = eightBallList.random()
        val x = (width - fontMetrics.stringWidth(sayThis)) / 2
        val y = fontMetrics.ascent + (height - fontMetrics.height) / 2
        drawString(sayThis, x, y)
    }
}
