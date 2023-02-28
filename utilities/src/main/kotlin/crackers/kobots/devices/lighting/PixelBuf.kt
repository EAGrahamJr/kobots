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

package crackers.kobots.devices.lighting

import crackers.kobots.devices.inRange
import java.awt.Color
import kotlin.math.roundToInt

/**
 * Wrapper for colors that includes an optional white level for devices that support that. An optional [brightness]
 * can also be applied.
 */
class PixelColor(val color: Color, val white: Int = 0, val brightness: Float? = null) : Cloneable {
    public override fun clone() = PixelColor(color, white, brightness)
}

/**
 * A re-implementation of the "pure Python" Adafruit PixelBuf class, which is a re-implementation of the base Adafruit
 * `pixelbuf` implementation.
 *
 * Note that some difference exist: for example, this relies on the base Java classes to represent colors vs the
 * Python `ColorUnion` construct that allows for 3 different types of "colors".
 *
 * TODO dotStar implementation is probably incorrect - needs device to test with
 */
abstract class PixelBuf(
    val size: Int, // size
    val byteOrder: String = "BGR",
    brightness: Float = 1f,
    autoWriteEnabled: Boolean = false,
    header: ByteArray? = null,
    trailer: ByteArray? = null
) {
    private val bpp: Int = byteOrder.length
    private val dotstarMode: Boolean
    private val hasWhite: Boolean
    private val colorOrder: List<Int>

    private val bufferOffset: Int
    private val effectiveSize: Int
    private val pixelStep: Int

    private val pixelBuffer: ByteArray
    private val currentColors: Array<PixelColor> = Array(size) { PixelColor(Color.BLACK, 0) }

    private val BUFFER_RANGE = 0 until size

    private var _autoWrite = autoWriteEnabled
    var autoWrite: Boolean
        get() = _autoWrite
        set(b) {
            _autoWrite = b
        }

    /**
     * Manage how bright things are
     */
    private var _brightness: Float = 0f
    var brightness: Float
        get() = _brightness
        set(b) {
            if (b < 0f || b > 1f) throw IllegalArgumentException("'brightness' is out of range (0.0-1.0)")
            val delta = b - _brightness
            _brightness = b

            // adjust brightness of existing values
            for (i in 0 until effectiveSize) {
                if (dotstarMode && (i + 1) % 4 == 0) continue
                val index = i + bufferOffset
                val item = pixelBuffer[index].toUByte().toFloat()
                val adjusted = (item * delta).roundToInt()
                pixelBuffer[index] = (item + adjusted).toInt().toByte()
            }
            if (_autoWrite) show()
        }

    init {
        byteOrder.length.inRange("byteOrder", 0..4)
        val r = byteOrder.indexOf("R").also {
            if (it < 0) throw IllegalArgumentException("byteOrder does not contain 'R'")
        }
        val g = byteOrder.indexOf("G").also {
            if (it < 0) throw IllegalArgumentException("byteOrder does not contain 'R'")
        }
        val b = byteOrder.indexOf("B").also {
            if (it < 0) throw IllegalArgumentException("byteOrder does not contain 'R'")
        }
        colorOrder = when {
            byteOrder.contains("W") -> {
                hasWhite = true
                dotstarMode = false
                listOf(r, g, b, byteOrder.indexOf("W"))
            }

            byteOrder.contains("P") -> {
                hasWhite = false
                dotstarMode = true
                val lum = byteOrder.indexOf("P")
                // TODO check this - seems odd
                // this makes no fucking sense because the original "parse" method is static?
                // NB - this shifts the bytes for the colors by one so the brightness can be set in the first byte for
                // each pixel
                listOf(r + 1, g + 1, b + 1, 0)
            }

            else -> {
                hasWhite = false
                dotstarMode = false
                listOf(r, g, b)
            }
        }

        pixelStep = if (dotstarMode) 4 else bpp
        effectiveSize = pixelStep * size
        val baseSize = effectiveSize + (header?.size ?: 0) + (trailer?.size ?: 0)

        pixelBuffer = ByteArray(baseSize)
        // if there's a header
        bufferOffset = if (header != null) {
            header.copyInto(pixelBuffer)
            header.size
        } else {
            0
        }
        // if there's a trailer
        trailer?.copyInto(pixelBuffer, bufferOffset + size)

        // pre-set the brightness value
        if (dotstarMode) {
            for (i in bufferOffset..effectiveSize step 4) pixelBuffer[i] = DOTSTAR_LED_START_FULL_BRIGHT
        }
        _brightness = brightness
    }

    /**
     * Fill the entire device with this color. If [autoWrite] is enabled, the results are immediately uploaded.
     */
    fun fill(color: PixelColor) {
        val parsed = parseColor(color)
        (0 until size).forEach { setItem(it, parsed) }
        if (_autoWrite) show()
    }

    fun fill(color: Color) {
        fill(PixelColor(color))
    }

    /**
     * Set an individual pixel (this is available as an _indexed_ value). If [autoWrite] is enabled, the results are
     * immediately uploaded.
     */
    operator fun set(index: Int, color: PixelColor) {
        setItem(index, parseColor(color))
        currentColors[index] = color
        if (_autoWrite) show()
    }

    operator fun set(index: Int, color: Color) {
        set(index, PixelColor(color))
    }

    /**
     * Set a range of pixels to a color. If [autoWrite] is enabled, the results are * immediately uploaded.
     */
    operator fun set(start: Int, end: Int, color: PixelColor) {
        val c = parseColor(color)
        (start..end).forEach {
            setItem(it, c)
            currentColors[it] = color
        }
        if (_autoWrite) show()
    }

    operator fun set(start: Int, end: Int, color: Color) {
        set(start, end, PixelColor(color))
    }

    /**
     * Set a range of pixels to a range of colors. The color list must be equal to or larger than the range specified.
     * If [autoWrite] is enabled, the results are * immediately uploaded.
     */
    operator fun set(start: Int, end: Int, colors: List<PixelColor>) {
        (start..end).forEach {
            setItem(it, parseColor(colors[it]))
            currentColors[it] = colors[it]
        }
        if (_autoWrite) show()
    }

    /**
     * Current color for the given pixel.
     */
    operator fun get(index: Int) = currentColors[index]

    /**
     * Immutable operator - changing these colors has no effect.
     */
    operator fun iterator(): Iterator<PixelColor> =
        object : Iterator<PixelColor> {
            var current = 0
            override fun hasNext() = current + 1 < size
            override fun next() = currentColors[++current].clone()
        }

    /**
     * Sends the current pixel buffer to the device.
     */
    fun show() {
        sendBuffer(pixelBuffer)
    }

    /**
     * Sets up an individual pixel element, with whiteness stuff and brightness
     */
    protected fun setItem(index: Int, color: PixelBufColor) {
        val offset = index.inRange("pixelIndex", BUFFER_RANGE) * bpp + bufferOffset
        val pixelBrightness = color.bright ?: _brightness

        if (bpp == 4) {
            // scale white if not dotStar
            val w = (color.white * (if (!dotstarMode) pixelBrightness else 1f)).roundToInt().toByte()
            pixelBuffer[offset + colorOrder[3]] = w
        }

        pixelBuffer[offset + colorOrder[0]] = color.red.brightness(pixelBrightness)
        pixelBuffer[offset + colorOrder[1]] = color.green.brightness(pixelBrightness)
        pixelBuffer[offset + colorOrder[2]] = color.blue.brightness(pixelBrightness)
    }

    protected abstract fun sendBuffer(buffer: ByteArray)

    /**
     * Dork with white values based on the setup.
     */
    protected fun parseColor(pixelColor: PixelColor): PixelBufColor {
        val color = pixelColor.color
        val whiteValue = pixelColor.white
        val bright = pixelColor.brightness

        val useWhite = hasWhite && Color.red == Color.green && Color.green == Color.blue

        // adjust white value
        val w = (
            if (bpp == 4) {
                // LED startframe is three "1" bits, followed by 5 brightness bits
                // then 8 bits for each of R, G, and B. The order of those 3 are configurable and
                // vary based on hardware
                if (dotstarMode) {
                    ((whiteValue * 31) and 0b00011111) or DOTSTAR_LED_START
                } else {
                    if (useWhite) {
                        whiteValue
                    } else {
                        0
                    }
                }
            }
            // not usable
            else {
                0
            }
            ).toByte()

        return if (useWhite) {
            PixelBufColor(0, 0, 0, w, bright)
        } else {
            PixelBufColor(color.red.toByte(), color.green.toByte(), color.blue.toByte(), w, bright)
        }
    }

    protected class PixelBufColor(val red: Byte, val green: Byte, val blue: Byte, val white: Byte, val bright: Float?)

    companion object {
        const val DOTSTAR_LED_START_FULL_BRIGHT = 0xFF.toByte()
        const val DOTSTAR_LED_START = 0b11100000

        fun Byte.brightness(b: Float): Byte = (this.toUByte().toFloat() * b).roundToInt().toByte()
    }
}
