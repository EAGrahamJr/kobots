/*
 * Copyright 2022-2022 by E. A. Graham, Jr.
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

package crackers.kobots.devices

import com.diozero.api.DeviceInterface
import com.diozero.devices.LcdConnection
import com.diozero.util.SleepUtil.sleepNanos

/**
 * Simplified LDC 2-row, 16-column display with integrated I<sup>2</sup>C controller: best guess for the hardware
 * identifier is "GH1602-2502".
 *
 * - Copied from [Pi4J/pi4j-example-components](https://github.com/Pi4J/pi4j-example-components), Apache v2 License.
 * - Uses the [diozero](https://github.com/mattjlewis/diozero) devices for actual GPIO interfacing and utilities
 */
class GH1602LCD(private val i2c: LcdConnection) : DeviceInterface {
    private val rows: Int = 2
    private val columns: Int = 16

    private fun Int.rowCheck() = require(this in 0 until rows) { "Row $this out of range (0 to $rows)" }
    private fun Int.columnCheck() = require(this in 0 until columns) { "Column $this out of range (0 to $columns)" }
    private fun String.lengthCheck() = require(this.length <= columns) { "String too long to fit" }

    // maintains the internal state of the cursor
    private var cursorDisplayControl = 0

    // controls the state of the backlight
    private var backlightBacker: Boolean = false
    var backlight: Boolean
        get() = backlightBacker
        set(value) {
            backlightBacker = value
            val setState = if (backlightBacker) BACKLIGHT_ON else BACKLIGHT_OFF
            executeCommand(setState)
        }

    // simple contextual syntax sugar
    val ON = true
    val OFF = false

    init {
        for (i in 1..3) writeCommand(0x03)
        writeCommand(0x02)

        // Initialize display settings
        writeCommand(FUNCTION_SET or MODE_2LINE or DOTS_5x8 or MODE_4BITS)
        writeCommand(DISPLAY_CONTROL or DISPLAY_ON or CURSOR_OFF or BLINK_OFF)
        writeCommand(ENTRY_MODE_SET or ENTRY_LEFT or ENTRY_SHIFT_DECREMENT)
        clearDisplay()

        // Enable backlight
        backlight = ON
    }

    override fun close() {
        off()
        i2c.close()
    }

    /**
     * "Homes" cursor (0,0)
     */
    fun moveCursorHome() {
        writeCommand(RETURN_HOME)
    }

    /**
     * Clear and move "home" (0,0)
     */
    fun clearDisplay() {
        writeCommand(CLEAR_DISPLAY)
        moveCursorHome()
    }

    /**
     * Shuts the display off
     */
    fun off() {
        executeCommand(DISPLAY_OFF)
    }

    /**
     * Turns the display on
     */
    fun on() {
        executeCommand(DISPLAY_ON)
    }

    /**
     * Write multi-line [text]
     */
    fun writeText(text: String) {
        require(text.length <= (rows * columns)) { "Text is too long: only ${rows * columns} characters allowed" }

        text.split("\n").forEachIndexed { row, line ->
            displayLine(line.padEnd(columns), row)
        }
    }

    /**
     * Write a single line of [text] to [row] (default 0) at [column] (default 0)
     */
    @JvmOverloads
    fun displayText(text: String, row: Int = 0, column: Int = 0) {
        row.rowCheck()
        column.columnCheck()

        // prepend text with column offset
        val textToDisplay = (" ".repeat(column) + text)
        textToDisplay.lengthCheck()
        // pad to end
        displayLine(textToDisplay.padEnd(columns), row)
    }

    /**
     * Clears [row] (writes blanks)
     */
    fun clearLine(row: Int) {
        row.rowCheck()
        displayLine(" ".repeat(columns), row)
    }

    /**
     * Write a single [character] to the current cursor location
     */
    fun writeCharacter(character: Char) {
        writeSplitCommand(character.code, REGISTER_SELECT_BIT)
    }

    /**
     * Write one of the 8 special [character] to the current cursor location
     */
    fun writeCharacter(character: Int) {
        require(character in 0..7) { "Only special characters 0-7 allowed" }
        writeSplitCommand(character, REGISTER_SELECT_BIT)
    }

    /**
     * displays a line on a specific position
     *
     * @param text to display
     * @param row  for the start of the text
     */
    private fun displayLine(text: String, row: Int) {
        writeCommand(SET_DDRAM_ADDR + LCD_ROW_OFFSETS[row])
        for (element in text) writeCharacter(element)
    }

    /**
     * Moves the cursor 1 character right
     */
    fun moveCursorRight() {
        executeCommand(CURSOR_SHIFT, (CURSOR_MOVE or MOVE_RIGHT))
    }

    /**
     * Moves the cursor 1 character left
     */
    fun moveCursorLeft() {
        executeCommand(CURSOR_SHIFT, (CURSOR_MOVE or MOVE_LEFT))
    }

    /**
     * Sets the cursor to a target destination
     *
     * @param column Selects the character of the line. Range: 0 - Columns-1
     * @param row  Selects the line of the display. Range: 1 - ROWS
     */
    fun setCursorToPosition(column: Int, row: Int) {
        row.rowCheck()
        column.columnCheck()
        writeCommand(SET_DDRAM_ADDR + column + LCD_ROW_OFFSETS[row] + column)
    }

    /**
     * Sets the display cursor to hidden or showing
     *
     * @param show Set the state of the cursor
     */
    fun setCursorVisibility(show: Boolean) {
        cursorDisplayControl = if (show) {
            (cursorDisplayControl or CURSOR_ON)
        } else {
            (cursorDisplayControl and CURSOR_ON.inv())
        }

        executeCommand(DISPLAY_CONTROL, cursorDisplayControl)
    }

    /**
     * Set the cursor to blinking or static
     *
     * @param blink Blink = true means the cursor will change to blinking mode. False lets the cursor stay static
     */
    fun setCursorBlinking(blink: Boolean) {
        cursorDisplayControl = if (blink) {
            (cursorDisplayControl or BLINK_ON)
        } else {
            (cursorDisplayControl and BLINK_ON.inv())
        }

        executeCommand(DISPLAY_CONTROL, cursorDisplayControl)
    }

    /**
     * Moves the whole displayed text one character right
     */
    fun moveDisplayRight() {
        executeCommand(CURSOR_SHIFT, (DISPLAY_MOVE or MOVE_RIGHT))
    }

    /**
     * Moves the whole displayed text one character right
     */
    fun moveDisplayLeft() {
        executeCommand(CURSOR_SHIFT, (DISPLAY_MOVE or MOVE_LEFT))
    }

    /**
     * Upload a custom character to the [location] (0 to 7): the [character] is described as a byte array, with each bit
     * set for a pixel.
     */
    fun createCharacter(location: Int, character: ByteArray) {
        TODO("Needs testing/validation")
//        require(character.size == 8) {
//            "Array has invalid length. Character is only 5x8 Digits. Only a array with length" +
//                    " 8 is allowed"
//        }
//        require(!(location > 7 || location < 1)) { "Invalid memory location. Range 1-7 allowed. Value: $location" }
//        writeCommand(SET_CGRAM_ADDR or (location shl 3))
//        for (i in 0..7) {
//            writeSplitCommand(character[i].toInt(), REGISTER_SELECT_BIT)
//        }
    }

    /**
     * Write a command to the LCD
     */
    private fun writeCommand(cmd: Int) {
        writeSplitCommand(cmd, 0)
    }

    /**
     * Write a command in 2 parts to the LCD
     */
    private fun writeSplitCommand(cmd: Int, mode: Int) {
        // bitwise AND with 11110000 to remove last 4 bits
        writeFourBits((mode or (cmd and 0xF0)).toByte())
        // bitshift and bitwise AND to remove first 4 bits
        writeFourBits((mode or (cmd shl 4 and 0xF0)).toByte())
    }

    /**
     * Write the four bits of a byte to the LCD
     *
     * @param data the byte that is sent
     */
    private fun writeFourBits(data: Byte) {
        val backlightSetting = if (backlight) BACKLIGHT_ON else BACKLIGHT_OFF
        i2c.write((data.toInt() or backlightSetting).toByte())
        strobe(data)
    }

    /**
     * Strobes the data to the LCD (writing to memory)
     */
    private fun strobe(data: Byte) {
        val backlightSetting = if (backlight) BACKLIGHT_ON else BACKLIGHT_OFF
        val sendData = data.toInt() or backlightSetting

        i2c.write((sendData or ENABLE_BIT).toByte())
        sleepNanos(0, 500_000)
        i2c.write((sendData and ENABLE_BIT.inv()).toByte())
        sleepNanos(0, 100_000)
    }

    /**
     * Execute two-part display codes with [command] and [data] (OR'd together)
     */
    private fun executeCommand(command: Int, data: Int) {
        executeCommand(command or data)
    }

    /**
     * Execute a direct command on the display
     */
    private fun executeCommand(cmd: Int) {
        i2c.write(cmd.toByte())
        sleepNanos(0, 100_000)
    }

    companion object {
        /** Flags for display commands  */
        private const val CLEAR_DISPLAY = 0x01
        private const val RETURN_HOME = 0x02
        private const val ENTRY_MODE_SET = 0x04
        private const val DISPLAY_CONTROL = 0x08
        private const val CURSOR_SHIFT = 0x10
        private const val FUNCTION_SET = 0x20
        private const val SET_CGRAM_ADDR = 0x40
        private const val SET_DDRAM_ADDR = 0x80

        // flags for display entry mode: left to right
        private const val ENTRY_LEFT = 0x02
        private const val ENTRY_SHIFT_DECREMENT = 0x00

        // flags for display entry mode: right to left
        private const val ENTRY_RIGHT = 0x00
        private const val ENTRY_SHIFT_INCREMENT = 0x01

        // flags for display on/off control
        private const val BACKLIGHT_ON = 0x08
        private const val BACKLIGHT_OFF = 0x00
        private const val DISPLAY_ON = 0x04
        private const val DISPLAY_OFF = 0x00
        private const val CURSOR_ON = 0x02
        private const val CURSOR_OFF = 0x00
        private const val BLINK_ON = 0x01
        private const val BLINK_OFF = 0x00

        // flags for display/cursor shift
        private const val DISPLAY_MOVE = 0x08
        private const val CURSOR_MOVE = 0x00
        private const val MOVE_RIGHT = 0x04
        private const val MOVE_LEFT = 0x00

        // flags for function set
        private const val MODE_8BITS = 0x10
        private const val MODE_4BITS = 0x00
        private const val MODE_2LINE = 0x08
        private const val MODE_1LINE = 0x00
        private const val DOTS_5x10 = 0x04
        private const val DOTS_5x8 = 0x00

        // flags for backlight control
        private const val ENABLE_BIT = 4 // Enable bit
        private const val Rw = 2.toByte() // Read/Write bit
        private const val REGISTER_SELECT_BIT = 1 // Register select bit

        /**
         * Display row offsets. Offset for up to 4 rows.
         */
        private val LCD_ROW_OFFSETS = intArrayOf(0x00, 0x40, 0x14, 0x54)
    }
}
