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

import com.diozero.api.RuntimeIOException
import com.diozero.devices.LcdConnection
import com.diozero.devices.LcdInterface
import com.diozero.util.SleepUtil

/**
 * 2- or 4-line LCD with integrated I2C controller based on the `diozero` library.
 *
 * Reference material:
 *
 *  * [Python Code](https://github.com/Freenove/Freenove_LCD_Module/tree/main/Freenove_LCD_Module_for_Raspberry_Pi/Python/Python_Code)
 *  * [Pi4J/pi4j-example-components](https://github.com/Pi4J/pi4j-example-components)", Apache v2 License.
 *
 */
class HD44780_Lcd(private val lcdConnection: LcdConnection, private val cols: Int, private val rows: Int) :
    LcdInterface {
    private var backlight = false
    private var displayOn = true
    private var cursorVisible = false
    private var cursorBlinking = false
    private var autoScroll = false
    private var leftToRight = true

    /**
     * Constructor.
     *
     * @param lcdConnection the connection to use (typically a "backpack" or direct GPIO)
     * @param fourLines     if `true`, the display is treated as 4-lines, 20-character display mode
     */
    init {

        // this serves as a "reset" in case the display is wonky
        writeCommand(RETURN_HOME or CLEAR_DISPLAY)
        writeCommand(RETURN_HOME or CLEAR_DISPLAY)
        writeCommand(RETURN_HOME or CLEAR_DISPLAY)
        writeCommand(RETURN_HOME)

        // init the interface (2 lines, 5x8, 4-bit mode) - note this is the same for the 4-line as well)
        writeCommand(LCD_FUNCTION_SET or LCD_2LINE or LCD_5x8DOTS or LCD_4BIT_MODE)

        // initialize display settings - display on, cursor off, blink off
        displayControl(true, false, false)
        // by default, set up for Indo-European left-to-right
        writeCommand(ENTRY_MODE_SET or ENTRY_RIGHT or ENTRY_DISABLE_SHIFT)

        // turn it on
        clear()
        isBacklightEnabled = true
    }

    override fun getColumnCount(): Int {
        return cols
    }

    override fun getRowCount(): Int {
        return rows
    }

    override fun isBacklightEnabled(): Boolean {
        return backlight
    }

    override fun setBacklightEnabled(backlightEnabled: Boolean): LcdInterface {
        backlight = backlightEnabled
        executeCommand(if (backlight) BACKLIGHT else 0)
        return this
    }

    override fun setCursorPosition(column: Int, row: Int): LcdInterface {
        rowCheck(row)
        columnCheck(column)
        writeCommand(SELECT_DDRAM_ADDR or column + LCD_ROW_OFFSETS[row])
        return this
    }

    override fun addText(character: Char): LcdInterface {
        writeSplitCommand(character.code, REGISTER_SELECT)
        return this
    }

    override fun addText(code: Int): LcdInterface {
        writeSplitCommand(code, REGISTER_SELECT)
        return this
    }

    override fun clear(): LcdInterface {
        writeCommand(CLEAR_DISPLAY)
        SleepUtil.sleepMillis(3)
        return this
    }

    override fun returnHome(): LcdInterface {
        writeCommand(RETURN_HOME)
        return this
    }

    override fun autoscrollOn(): LcdInterface {
        return entryControl(true, leftToRight)
    }

    override fun autoscrollOff(): LcdInterface {
        return entryControl(false, leftToRight)
    }

    fun rightToLeft(): LcdInterface {
        return entryControl(autoScroll, false)
    }

    fun leftToRight(): LcdInterface {
        return entryControl(autoScroll, true)
    }

    override fun isIncrementOn(): Boolean {
        return !autoScroll
    }

    override fun isShiftDisplayOn(): Boolean {
        return autoScroll
    }

    fun entryControl(autoScroll: Boolean, leftToRight: Boolean): LcdInterface {
        this.autoScroll = autoScroll
        this.leftToRight = leftToRight
        writeCommand(
            ENTRY_MODE_SET or
                    (if (leftToRight) ENTRY_RIGHT else ENTRY_LEFT) or
                    if (autoScroll) ENTRY_ENABLE_SHIFT else ENTRY_DISABLE_SHIFT
        )
        return this
    }

    override fun displayControl(displayOn: Boolean, cursorEnabled: Boolean, blinkEnabled: Boolean): LcdInterface {
        this.displayOn = displayOn
        cursorVisible = cursorEnabled
        cursorBlinking = blinkEnabled
        val command = DISPLAY_CONTROL or
                (if (displayOn) DISPLAY_ON else 0) or
                (if (cursorVisible) CURSOR_ON else 0) or
                if (cursorBlinking) BLINK_ON else 0
        writeCommand(command)
        return this
    }

    override fun displayOn(): LcdInterface {
        displayControl(true, cursorVisible, cursorBlinking)
        return this
    }

    override fun displayOff(): LcdInterface {
        displayControl(false, cursorVisible, cursorBlinking)
        return this
    }

    override fun cursorOn(): LcdInterface {
        displayControl(displayOn, true, cursorBlinking)
        return this
    }

    override fun cursorOff(): LcdInterface {
        displayControl(displayOn, false, cursorBlinking)
        return this
    }

    override fun blinkOn(): LcdInterface {
        displayControl(displayOn, cursorVisible, true)
        return this
    }

    override fun blinkOff(): LcdInterface {
        displayControl(displayOn, cursorVisible, false)
        return this
    }

    override fun isCursorEnabled(): Boolean {
        return cursorVisible
    }

    override fun isBlinkEnabled(): Boolean {
        return cursorBlinking
    }

    override fun shiftDisplayRight(): LcdInterface {
        writeCommand(MOVE_CONTROL or DISPLAY_MOVE or MOVE_RIGHT)
        return this
    }

    override fun shiftDisplayLeft(): LcdInterface {
        writeCommand(MOVE_CONTROL or DISPLAY_MOVE or MOVE_LEFT)
        return this
    }

    override fun moveCursorRight(): LcdInterface {
        writeCommand(MOVE_CONTROL or CURSOR_MOVE or MOVE_RIGHT)
        return this
    }

    override fun moveCursorLeft(): LcdInterface {
        writeCommand(MOVE_CONTROL or CURSOR_MOVE or MOVE_LEFT)
        return this
    }

    override fun createChar(location: Int, charMap: ByteArray): LcdInterface {
        require(!(location < 0 || location > 7)) { "Invalid location ($location) , must be 0..7" }
        require(charMap.size == 8) { "Invalid charMap length (" + charMap.size + ") , must be 8" }
        writeCommand(SELECT_CGRAM_ADDR or (location shl 3))
        for (i in 0..7) {
            writeSplitCommand(charMap[i].toInt(), REGISTER_SELECT)
        }
        return this
    }

    @Throws(RuntimeIOException::class)
    override fun close() {
        backlight = false
        displayOff()
    }

    /**
     * Write a command to the LCD
     */
    private fun writeCommand(cmd: Int) {
        writeSplitCommand(cmd, 0)
    }

    /**
     * Write a command in nibbles
     */
    private fun writeSplitCommand(cmd: Int, mode: Int) {
        writeFourBits(mode or (cmd and 0xF0))
        writeFourBits(mode or (cmd shl 4 and 0xF0))
    }

    /**
     * Write the four bits of a byte, with backlight control and enable on/off
     *
     * @param data the byte that is sent
     */
    private fun writeFourBits(data: Int) {
        val sendData = data or if (backlight) BACKLIGHT else NO_BACKLIGHT
        lcdConnection.write(sendData.toByte())
        lcdConnection.write((sendData or ENABLE_BIT).toByte())
        SleepUtil.sleepNanos(0, 50_000)
        lcdConnection.write((sendData and ENABLE_BIT.inv()).toByte())
        SleepUtil.sleepNanos(0, 50_000)
    }

    /**
     * Execute a direct command on the display - typically for hardware control
     */
    private fun executeCommand(cmd: Int) {
        lcdConnection.write(cmd.toByte())
        SleepUtil.sleepNanos(0, 50_000)
    }

    companion object {
        // display commands
        private const val CLEAR_DISPLAY = 0x01
        private const val RETURN_HOME = 0x02

        // RAM select
        private const val SELECT_CGRAM_ADDR = 0x40
        private const val SELECT_DDRAM_ADDR = 0x80

        // row offsets (OR with DDRAM select to write to the display)
        private val LCD_ROW_OFFSETS = byteArrayOf(0x00, 0x40, 0x14, 0x54)

        // display entry mode
        private const val ENTRY_MODE_SET = 0x04 // control bit
        private const val ENTRY_LEFT = 0x00
        private const val ENTRY_RIGHT = 0x02 // moves cursor to the right after write (increase RAM address by 1)

        // shift display on entry
        private const val ENTRY_ENABLE_SHIFT = 0x01
        private const val ENTRY_DISABLE_SHIFT = 0x00

        // display on/off controls
        private const val DISPLAY_CONTROL = 0x08 // control bit
        private const val DISPLAY_ON = 0x04
        private const val CURSOR_ON = 0x02
        private const val BLINK_ON = 0x01

        // display/cursor shift
        private const val MOVE_CONTROL = 0x10 // control bit
        private const val DISPLAY_MOVE = 0x08
        private const val CURSOR_MOVE = 0x00
        private const val MOVE_RIGHT = 0x04
        private const val MOVE_LEFT = 0x00

        // function set
        private const val LCD_FUNCTION_SET = 0x20 // control bit
        private const val LCD_8BIT_MODE = 0x10
        private const val LCD_4BIT_MODE = 0x00
        private const val LCD_2LINE = 0x08

        // N.B. these are not a thing for this
        private const val LCD_1LINE = 0x00
        private const val LCD_5x10DOTS = 0x04
        private const val LCD_5x8DOTS = 0x00

        // backlight control
        private const val BACKLIGHT = 0x08
        private const val NO_BACKLIGHT = 0x00

        // data transfer
        private const val ENABLE_BIT = 0x04 // Enable bit
        private const val RW_BIT = 0x02 // Read/Write bit
        private const val REGISTER_SELECT = 0x01 // Register select bit

        /**
         * Backpack
         */
        val PiBackpack by lazy { LcdConnection.PCF8574LcdConnection(1) }

        /**
         * Default 2-line deisplay
         */
        val Pi2Line by lazy { HD44780_Lcd(PiBackpack, 16, 2) }

        /**
         * Default 4-line display
         */
        val Pi4Line by lazy { HD44780_Lcd(PiBackpack, 20, 4) }
    }
}
