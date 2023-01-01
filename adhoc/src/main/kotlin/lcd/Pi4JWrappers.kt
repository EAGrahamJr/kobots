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

package lcd

import com.diozero.devices.LcdInterface
import com.pi4j.HackLcd
import com.pi4j.catalog.components.LcdDisplay

/*
 * Wrappers around Pi4J LCD to make them look like diozero devices for single tests.
 */
// hacked up with diozero GPIO class instead of Pi4J context
fun hackWrapper(delegate: HackLcd): LcdInterface = object : LcdInterface {
    override fun close() {
        delegate.close()
    }

    override fun getColumnCount(): Int {
        return delegate.columns
    }

    override fun getRowCount(): Int {
        return delegate.rows
    }

    override fun isBacklightEnabled(): Boolean {
        return delegate.backlight
    }

    override fun setBacklightEnabled(p0: Boolean): LcdInterface {
        delegate.setDisplayBacklight(p0)
        return this
    }

    override fun setCursorPosition(column: Int, row: Int): LcdInterface {
        delegate.setCursorToPosition(column, row)
        return this
    }

    override fun addText(char: Char): LcdInterface {
        delegate.writeCharacter(char)
        return this
    }

    override fun addText(code: Int): LcdInterface {
        delegate.writeCharacter(code.toChar())
        return this
    }

    override fun clear(): LcdInterface {
        delegate.clearDisplay()
        return this
    }

    override fun returnHome(): LcdInterface {
        delegate.moveCursorHome()
        return this
    }

    override fun autoscrollOn(): LcdInterface {
        FAIL("autoscroll")
        return this
    }

    override fun autoscrollOff(): LcdInterface {
        FAIL("autoscroll")
        return this
    }

    override fun isIncrementOn(): Boolean {
        FAIL("increment")
        return false
    }

    override fun isShiftDisplayOn(): Boolean {
        FAIL("shift display")
        return false
    }

    override fun displayControl(displayOn: Boolean, cursorEnabled: Boolean, blinkEnabled: Boolean): LcdInterface {
        delegate.setCursorVisibility(cursorEnabled)
        delegate.setCursorBlinking(blinkEnabled)
        return this
    }

    override fun displayOn(): LcdInterface {
        return this
    }

    override fun displayOff(): LcdInterface {
        delegate.off()
        return this
    }

    override fun cursorOn(): LcdInterface {
        delegate.setCursorVisibility(true)
        return this
    }

    override fun cursorOff(): LcdInterface {
        delegate.setCursorVisibility(false)
        return this
    }

    override fun blinkOn(): LcdInterface {
        delegate.setCursorBlinking(true)
        return this
    }

    override fun blinkOff(): LcdInterface {
        delegate.setCursorBlinking(false)
        return this
    }

    override fun isCursorEnabled(): Boolean {
        FAIL("get cursor")
        return false
    }

    override fun isBlinkEnabled(): Boolean {
        FAIL("get cursor")
        return false
    }

    override fun shiftDisplayRight(): LcdInterface {
        delegate.moveDisplayRight()
        return this
    }

    override fun shiftDisplayLeft(): LcdInterface {
        delegate.moveDisplayLeft()
        return this
    }

    override fun moveCursorRight(): LcdInterface {
        delegate.moveCursorRight()
        return this
    }

    override fun moveCursorLeft(): LcdInterface {
        delegate.moveCursorLeft()
        return this
    }

    override fun createChar(position: Int, data: ByteArray?): LcdInterface {
        delegate.createCharacter(position, data)
        return this
    }
}

// hacked up with diozero GPIO class instead of Pi4J context
fun pi4JWrapper(delegate: LcdDisplay): LcdInterface = object : LcdInterface {
    override fun close() {
        // no equivalent
    }

    override fun getColumnCount(): Int {
        return delegate.columns
    }

    override fun getRowCount(): Int {
        return delegate.rows
    }

    override fun isBacklightEnabled(): Boolean {
        return delegate.backlight
    }

    override fun setBacklightEnabled(p0: Boolean): LcdInterface {
        delegate.setDisplayBacklight(p0)
        return this
    }

    override fun setCursorPosition(column: Int, row: Int): LcdInterface {
        delegate.setCursorToPosition(column, row + 1)
        return this
    }

    override fun addText(char: Char): LcdInterface {
        delegate.writeCharacter(char)
        return this
    }

    override fun addText(code: Int): LcdInterface {
        delegate.writeCharacter(code.toChar())
        return this
    }

    override fun clear(): LcdInterface {
        delegate.clearDisplay()
        return this
    }

    override fun returnHome(): LcdInterface {
        delegate.moveCursorHome()
        return this
    }

    override fun autoscrollOn(): LcdInterface {
        FAIL("autoscroll")
        return this
    }

    override fun autoscrollOff(): LcdInterface {
        FAIL("autoscroll")
        return this
    }

    override fun isIncrementOn(): Boolean {
        FAIL("increment")
        return false
    }

    override fun isShiftDisplayOn(): Boolean {
        FAIL("shiftdisplay")
        return false
    }

    override fun displayControl(displayOn: Boolean, cursorEnabled: Boolean, blinkEnabled: Boolean): LcdInterface {
        delegate.setCursorVisibility(cursorEnabled)
        delegate.setCursorBlinking(blinkEnabled)
        return this
    }

    override fun displayOn(): LcdInterface {
        return this
    }

    override fun displayOff(): LcdInterface {
        delegate.off()
        return this
    }

    override fun cursorOn(): LcdInterface {
        delegate.setCursorVisibility(true)
        return this
    }

    override fun cursorOff(): LcdInterface {
        delegate.setCursorVisibility(false)
        return this
    }

    override fun blinkOn(): LcdInterface {
        delegate.setCursorBlinking(true)
        return this
    }

    override fun blinkOff(): LcdInterface {
        delegate.setCursorBlinking(false)
        return this
    }

    override fun isCursorEnabled(): Boolean {
        FAIL("get cursor")
        return false
    }

    override fun isBlinkEnabled(): Boolean {
        FAIL("get cursor")
        return false
    }

    override fun shiftDisplayRight(): LcdInterface {
        delegate.moveDisplayRight()
        return this
    }

    override fun shiftDisplayLeft(): LcdInterface {
        delegate.moveDisplayLeft()
        return this
    }

    override fun moveCursorRight(): LcdInterface {
        delegate.moveCursorRight()
        return this
    }

    override fun moveCursorLeft(): LcdInterface {
        delegate.moveCursorLeft()
        return this
    }

    override fun createChar(position: Int, data: ByteArray?): LcdInterface {
        delegate.createCharacter(position + 1, data)
        return this
    }
}
