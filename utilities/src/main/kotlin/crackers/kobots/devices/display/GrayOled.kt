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

import com.diozero.api.DeviceInterface
import com.diozero.devices.oled.SsdOledCommunicationChannel
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.awt.image.ColorConvertOp
import java.awt.image.DataBufferByte

/**
 * Abstract B/W display.
 *
 * TODO Both C++ and Python use an in-memory buffer and only write to the display when explicitly told to do so.
 * TODO Because Java has a separate image construct, is this necessary?
 */
abstract class GrayOled(
    val delegate: SsdOledCommunicationChannel,
    val width: Int,
    val height: Int,
    val displayType: DisplayType,
    val initializationSequence: IntArray = IntArray(0),
    reset: Boolean = true
) : DeviceInterface {

    enum class DisplayType(val bitsPerPixel: Int) {
        WHITE(1), BLACK(1), INVERSE(1), FOUR_BITS(4)
    }

    private val i2c_dev = delegate is I2cCommunicationChannel

    protected abstract fun dataCommand(): Int

    init {
        if (reset && initializationSequence.isNotEmpty()) oledSendCommandList(initializationSequence)
    }

    override fun close() {
        delegate.close()
    }

    abstract fun setDisplayOn(on: Boolean)
    abstract fun invertDisplay(invert: Boolean)
    open fun getNativeImageType() = BufferedImage.TYPE_BYTE_GRAY

    /**
     * Display an image. This is the preferred method?
     */
    fun display(image: BufferedImage) {
        val scaledImage =
            if (image.width == width && image.height == height) image
            else {
                // scale to fit
                // TODO figure out a "scaling factor"?
                BufferedImage(width, height, image.type).apply {
                    createGraphics().apply {
                        drawImage(image, 0, 0, width, height, null)
                        dispose()
                    }
                }
            }

        displayImage(
            // if it already matches, just use it, otherwise convert to the default color space
            if (image.type.equals(getNativeImageType())) scaledImage
            else ColorConvertOp(image.colorModel.colorSpace, COLOR_SPACE, null).filter(scaledImage, null)
        )
    }

    /**
     * The main methodology for pushing the image to the on-board display memory
     */
    open protected fun displayImage(image: BufferedImage) {
        // TODO this is supposed to represent the "dirty buffer", initialized here to the display
        val window_x1 = 0
        val window_x2 = width - 1
        val window_y1 = 0
        val window_y2 = height - 1

        // ushort count = WIDTH * ((HEIGHT + 7) / 8);
        val rows = height
        val bytesPerRow = displayType.bitsPerPixel // because uses 4 bits, so two pixels per byte

        val row_start = Math.min(bytesPerRow - 1, (window_x1 / 2))
        val row_end = Math.max(0, (window_x2 / 2))
        val first_row = Math.min((rows - 1), window_y1)
        val last_row = Math.max(0, window_y2)

        // this defines the memory "window" that corresponds to the "dirty buffer" and resets the data RAM to where
        // we want to start writing
        val cmd = intArrayOf(
            SSD1327.SETROW,
            first_row,
            last_row,
            SSD1327.SETCOLUMN,
            row_start,
            row_end
        )
        oledSendCommandList(cmd)

        // start grabbing pixels
        val rasterized = (image.raster.dataBuffer as DataBufferByte).data
        // TODO because there's no specific color type for b/w,
        // TODO the non-gray images will need to force the values to 0 or 1
        when (displayType) {
            DisplayType.WHITE -> {
                for (offset in 0 until rasterized.size) sendDataBuffer(rasterized[offset].toInt())
            }

            DisplayType.BLACK -> {
                for (offset in 0 until rasterized.size) sendDataBuffer((rasterized[offset].toInt()).inv() and 0x00FF)
            }

            DisplayType.INVERSE -> {
                TODO("Haven't seen one of these")
            }

            DisplayType.FOUR_BITS -> {
                for (offset in 0 until rasterized.size step 2) {
                    val hiPixel = ((rasterized[offset] / 4) shl 4) and 0xF0
                    val loPixel = (rasterized[offset + 1] / 4) and 0x0F
                    sendDataBuffer(hiPixel or loPixel)
                }
            }
        }


//        if (i2c_dev) { // I2C
//            // Set low speed clk
//            i2c_dev->setSpeed(i2c_postclk);
//        }

        // reset dirty window
//        window_x1 = 1024;
//        window_y1 = 1024;
//        window_x2 = -1;
//        window_y2 = -1;
    }

    protected var sendBuffer = mutableListOf<Int>()
    protected fun sendDataBuffer(value: Int?) {
        if (value == null) {
            if (sendBuffer.isNotEmpty()) oledSendData(SSD1327.DATA_MODE, sendBuffer.toIntArray())
        } else {
            sendBuffer += value
            if (sendBuffer.size == 1024) {
                oledSendData(SSD1327.DATA_MODE, sendBuffer.toIntArray())
                sendBuffer.clear()
            }
        }
    }

    /**
     * Issue single command byte to OLED
     * @param c The single byte command
     */
    protected fun oledSendCommand(c: Int) = oledSendCommandList(intArrayOf(c))

    /**
     * Issue multiple bytes of commands OLED, using I2C or hard/soft SPI as
     * needed.
     *  @param values the commands to write
     */
    protected fun oledSendCommandList(values: IntArray) =
        if (i2c_dev) writeToI2C(CMD_MODE, values) else TODO("Add SPI support")

    protected fun oledSendData(dataCommand: Int, dataBuffer: IntArray) =
        if (i2c_dev) writeToI2C(dataCommand, dataBuffer) else TODO("Add SPI support")

    private fun writeToI2C(prefix: Int, buffer: IntArray) {
        val remapped: List<Byte> = mutableListOf(prefix.toByte())
        buffer.forEach { remapped + it.toByte() }
        delegate.write(*remapped.toByteArray())
    }

    companion object {
        const val CMD_MODE = 0x00
        const val BLACK = 0x0
        const val WHITE = 0xF

        private val COLOR_SPACE = ColorSpace.getInstance(ColorSpace.CS_GRAY)
    }
}
