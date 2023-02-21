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

package crackers.kobots.utilities

import java.awt.*
import java.awt.geom.*
import kotlin.math.floor

/**
 * A very simple circular- or arc-style "gauge", without resorting to large libraries.
 */
class Gauge @JvmOverloads constructor(
    val graphics: Graphics2D,
    val width: Int,
    val height: Int,
    val label: String? = null,
    val minimumValue: Double = 0.0,
    val maximumValue: Double = 100.0,
    shape: Shape = Shape.SEMICIRCLE,
    val foreground: Color = Color.WHITE,
    val background: Color = Color.BLACK,
    val font: String = Font.SANS_SERIF,
    val fontColor: Color = Color.WHITE
) {
    enum class Shape {
        CIRCLE, SEMICIRCLE
    }

    private var value = 0.0

    private val degrees: Double
    private val degreesPerTick: Double
    private val labelIncrement: Double
    private val radius: Double
    private val xMidpoint: Double
    private val yMidpoint: Double
    private val maxLabelRange: Int
    private val graphShape: RectangularShape

    init {
        // Set up the Graphics2D object
        graphics.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        )

        // Determine the geometry based on the type/shape of the gauge
        graphShape = if (shape == Shape.CIRCLE) {
            radius = Math.min(width / 2.0, height / 2.0)
            xMidpoint = width / 2.0
            yMidpoint = height / 2.0
            degrees = 360.0
            degreesPerTick = degrees / TICKS
            labelIncrement = (maximumValue - minimumValue) / TICKS
            maxLabelRange = TICKS
            Ellipse2D.Double(
                xMidpoint - radius, yMidpoint - radius,
                radius * 2.0, radius * 2.0
            )
        } else {
            radius = Math.min(width / 2.0, height.toDouble())
            xMidpoint = width / 2.0
            yMidpoint = height.toDouble() * .9 // move it off the bottom a little bit
            degrees = 180.0
            degreesPerTick = degrees / TICKS
            labelIncrement = (maximumValue - minimumValue) / TICKS
            maxLabelRange = TICKS + 1
            Arc2D.Double(
                xMidpoint - radius, yMidpoint - radius,
                radius * 2.0, radius * 2.0,
                0.0, degrees, Arc2D.OPEN
            )
        }

        graphics.color = foreground
        graphics.font = Font(font, Font.PLAIN, floor(height / 11.0).toInt()) // about 9% size
    }

    // defines the middle section of the dial
    private val middle = Ellipse2D.Double(
        xMidpoint - radius + TICK_LENGTH,
        yMidpoint - radius + TICK_LENGTH,
        (radius - TICK_LENGTH) * 2.0,
        (radius - TICK_LENGTH) * 2.0
    )

    // this forms the tick marks - the `middle` is drawn over these radial lines
    private val radial = Line2D.Double(xMidpoint - radius, yMidpoint, xMidpoint, yMidpoint)

    // the little dot in the center and the pointer
    private val pivot = Ellipse2D.Double(
        xMidpoint - PIVOT_RADIUS,
        yMidpoint - PIVOT_RADIUS,
        PIVOT_RADIUS * 2.0,
        PIVOT_RADIUS * 2.0
    )
    private val pointer = Line2D.Double(xMidpoint - radius + 2 * TICK_LENGTH, yMidpoint, xMidpoint, yMidpoint)

    // font start and how big stuff is
    private val labelStartPoint = Point2D.Double(xMidpoint - radius + 2 * TICK_LENGTH, yMidpoint)
    private val fontMetrics = graphics.fontMetrics

    /**
     * Render this crackers.kobots.utilities.Gauge
     *
     * @param g   The graphics object to use
     */
    fun paint() {
        // draw the circle/arc
        graphics.color = foreground
        graphics.stroke = OUTLINE_STROKE
        graphics.draw(graphShape)

        // draw tick-marks
        graphics.color = foreground
        graphics.stroke = TICK_STROKE
        var a = 0.0

        for (i in 0..TICKS) {
            val at = AffineTransform.getRotateInstance(
                Math.toRadians(a),
                xMidpoint, yMidpoint
            )
            graphics.draw(at.createTransformedShape(radial))
            a += degreesPerTick
        }

        // fill in the middle of the gauge - this chops off the above lines to make tick-marks ------------------------
        // TODO Fill the highlight
//        if (highlight != null) {
//            gradientColors = arrayOf(background, background)
//            gradientFractions = floatArrayOf(0.00f,0.80f)
//            gradient = RadialGradientPaint(
//                xMid.toFloat(), yMid.toFloat(), radius.toFloat(),
//                gradientFractions,
//                gradientColors
//            )
//            graphics.paint = gradient
//            graphics.fill(middle)
//        }

        graphics.color = background
        graphics.fill(middle)

        // Draw the text ----------------------------------------------------------------------------------------------
        graphics.color = fontColor

        if (label != null) {
            val stringWidth = fontMetrics.getStringBounds(label, graphics)
            graphics.drawString(
                label,
                (xMidpoint - stringWidth.width / 2f).toFloat(),
                (yMidpoint - PIVOT_RADIUS - fontMetrics.descent).toFloat()
            )
        }

        // and then the ticks
        a = 0.0
        var label = 0.0
        val q = Point2D.Double()
        for (i in 0 until maxLabelRange) {
            val nextTransformation = AffineTransform.getRotateInstance(
                Math.toRadians(a),
                xMidpoint, yMidpoint
            )
            nextTransformation.transform(labelStartPoint, q)
            val printThis = String.format("%3.0f", label + minimumValue).trim()
            val labelBounds = fontMetrics.getStringBounds(printThis, graphics).let {
                it.width.toFloat() to it.height.toFloat()
            }
            graphics.drawString(
                printThis,
                q.x.toFloat() - labelBounds.first / 2f, // center it
                q.y.toFloat() + labelBounds.second / 4f // move "up" approx. 1/4 font size
            )
            a += degreesPerTick
            label += labelIncrement
        }


        // Draw the pointer
        val at = AffineTransform.getRotateInstance(
            Math.toRadians(value / maximumValue * degrees),
            xMidpoint, yMidpoint
        )
        graphics.color = if (background != Color.BLACK) Color.BLACK else Color.WHITE
        graphics.draw(at.createTransformedShape(pointer))
        graphics.fill(pivot)
    }

    /**
     * Set the current value to display
     *
     * @param value  The current value
     */
    fun setValue(value: Double) {
        this.value = Math.min(maximumValue, Math.max(minimumValue, value))
    }

    companion object {
        private const val PIVOT_RADIUS = 4
        private const val TICK_LENGTH = 10
        private const val TICKS: Int = 10

        private val OUTLINE_STROKE: Stroke = BasicStroke(2.0f)
        private val TICK_STROKE: Stroke = BasicStroke(1.0f)
    }
}
