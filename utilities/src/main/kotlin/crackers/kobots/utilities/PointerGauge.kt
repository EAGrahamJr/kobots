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

import crackers.kobots.utilities.PointerGauge.Shape
import java.awt.*
import java.awt.geom.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * A very simple circular- or arc-style "gauge" (e.g. "dial", "dial pointer"), without resorting to large libraries.
 *
 * ## Specifying the drawing space
 * - [graphics] = the canvas to draw on
 * - [width], [height] = the drawing area size
 *
 * or
 * - [image] = a _rendered_ image
 *
 * ## Graph values
 *
 * - [minimumValue], [maximumValue] = range of expected values: the pointer won't go beyond these (default **0-100**)
 * - [label] = optional label in the center of the dial
 * - [shape] = either [Shape.SEMICIRCLE] (default) or [Shape.CIRCLE]
 * - [foreground] = color for the dial, ticks, and pointer (default `Color.WHITE`)
 * - [background] = color of the middle of the dial (default `Color.BLACK`)
 * - [font] = name of the font to use for the tick labels and the optional dial label (default "SansSerif")
 * - [fontColor] = color for said font (defaults to `foreground`)
 */
class PointerGauge @JvmOverloads constructor(
    private val graphics: Graphics2D,
    width: Int,
    height: Int,
    private val minimumValue: Double = 0.0,
    private val maximumValue: Double = 100.0,
    private val label: String? = null,
    shape: Shape = Shape.SEMICIRCLE,
    private val foreground: Color = Color.WHITE,
    private val background: Color = Color.BLACK,
    font: String = Font.SANS_SERIF,
    private val fontColor: Color = foreground
) {
    enum class Shape {
        CIRCLE, SEMICIRCLE
    }

    /**
     * Alternate constructor: uses the image to derive the graphics and dimensions.
     */
    @JvmOverloads
    constructor(
        image: Image,
        minimumValue: Double = 0.0,
        maximumValue: Double = 0.0,
        label: String? = null,
        shape: Shape = Shape.SEMICIRCLE,
        foreground: Color = Color.WHITE,
        background: Color = Color.BLACK,
        font: String = Font.SANS_SERIF,
        fontColor: Color = foreground
    ) :
        this(
            image.graphics as Graphics2D,
            image.getWidth(null),
            image.getHeight(null),
            minimumValue,
            maximumValue,
            label,
            shape,
            foreground,
            background,
            font,
            fontColor
        )

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
            radius = min(width / 2.0, height / 2.0)
            xMidpoint = width / 2.0
            yMidpoint = height / 2.0
            degrees = 360.0
            degreesPerTick = degrees / TICKS
            labelIncrement = (maximumValue - minimumValue) / TICKS
            maxLabelRange = TICKS
            Ellipse2D.Double(xMidpoint - radius, yMidpoint - radius, radius * 2.0, radius * 2.0)
        } else {
            radius = min(width / 2.0, height.toDouble())
            xMidpoint = width / 2.0
            yMidpoint = height.toDouble() * .9 // move it off the bottom a little bit
            degrees = 180.0
            degreesPerTick = degrees / TICKS
            labelIncrement = (maximumValue - minimumValue) / TICKS
            maxLabelRange = TICKS + 1
            Arc2D.Double(xMidpoint - radius, yMidpoint - radius, radius * 2.0, radius * 2.0, 0.0, degrees, Arc2D.OPEN)
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

    private val backgroundIsDrawn = AtomicBoolean(false)

    /**
     * Render the gauge.
     */
    fun paint() {
        // only do the "background" (e.g. nothing in the middle) once -------------------------------------------------
        if (!backgroundIsDrawn.compareAndSet(false, true)) {
            // draw the circle/arc
            graphics.color = foreground
            graphics.stroke = OUTLINE_STROKE
            graphics.draw(graphShape)

            // draw tick-marks (actually radial spokes - see next)
            graphics.color = foreground
            graphics.stroke = TICK_STROKE
            var tickMarkRotationAngle = 0.0

            for (i in 0..TICKS) {
                val at = AffineTransform.getRotateInstance(
                    Math.toRadians(tickMarkRotationAngle),
                    xMidpoint,
                    yMidpoint
                )
                graphics.draw(at.createTransformedShape(radial))
                tickMarkRotationAngle += degreesPerTick
            }
        }

        // fill in the middle of the gauge - this chops off the above lines to make tick-marks ------------------------
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

        // and then the tick labels
        var textRotationAngles = 0.0
        var label = minimumValue
        for (i in 0 until maxLabelRange) {
            val textLocation = AffineTransform.getRotateInstance(
                Math.toRadians(textRotationAngles),
                xMidpoint,
                yMidpoint
            ).transform(labelStartPoint, null)

            val printThis = String.format("%3.0f", label).trim()
            val labelBounds = fontMetrics.getStringBounds(printThis, graphics).let {
                it.width.toFloat() to it.height.toFloat()
            }
            graphics.drawString(
                printThis,
                textLocation.x.toFloat() - labelBounds.first / 2f, // center it
                textLocation.y.toFloat() + labelBounds.second / 4f // move "up" approx. 1/4 font size
            )
            textRotationAngles += degreesPerTick
            label += labelIncrement
        }

        // Draw the pointer
        val at = AffineTransform.getRotateInstance(
            Math.toRadians(value / maximumValue * degrees),
            xMidpoint,
            yMidpoint
        )
        graphics.color = foreground
        graphics.draw(at.createTransformedShape(pointer))
        graphics.fill(pivot)
    }

    /**
     * Set the current value to display
     *
     * @param value  The current value
     */
    fun setValue(value: Double) {
        this.value = min(maximumValue, max(minimumValue, value))
    }

    companion object {
        private const val PIVOT_RADIUS = 4
        private const val TICK_LENGTH = 10
        private const val TICKS: Int = 10

        private val OUTLINE_STROKE: Stroke = BasicStroke(2.0f)
        private val TICK_STROKE: Stroke = BasicStroke(1.0f)
    }
}
