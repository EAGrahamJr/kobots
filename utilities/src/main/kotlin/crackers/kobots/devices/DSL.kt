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

package crackers.kobots.devices

import com.diozero.api.DigitalOutputDevice
import com.diozero.api.PwmOutputDevice
import com.diozero.api.ServoDevice
import com.diozero.devices.LcdInterface
import com.diozero.devices.motor.MotorInterface
import kotlin.math.abs

/**
 * Adds DSL-like capabilities to `diozero`
 */

infix fun PwmOutputDevice.set(sensorValue: Boolean) {
    value = if (sensorValue) 1f else 0f
}

infix fun PwmOutputDevice.set(sensorValue: Float) {
    value = sensorValue
}

infix fun DigitalOutputDevice.set(input: Boolean) {
    setValue(input)
}

/**
 * Sets the value for the servo (-1 to 1).
 */
infix fun ServoDevice.set(input: Float) {
    value = input
}

infix fun ServoDevice.set(input: Int) = set(input.toFloat())

fun ServoDevice.setAngle(input: Int) = set(input.toFloat())

/**
 * Sets the [angle] for the servo. Note that this **must** be in the
 * range the servo supports.
 */
infix fun ServoDevice.at(angle: Float) {
    this.angle = angle
}

infix fun ServoDevice.at(angle: Int) = at(angle.toFloat())

/**
 * Uses array notion to set [text] on a [row]
 *
 * Example: `lcd[2] = "Hello, World"`
 */
operator fun LcdInterface<*>.set(row: Int, text: String) {
    setText(row, text)
}

/**
 * Use a Pair to express row, column
 *
 * Example: `lcd position Pair(1,1)`
 *
 * TODO not happy about this one
 */
infix fun LcdInterface<*>.position(location: Pair<Int, Int>) {
    setCursorPosition(location.second, location.first)
}

/**
 * Add [text] at current location
 */
operator fun LcdInterface<*>.plusAssign(text: String) {
    addText(text)
}

/**
 * Add [char] at current location
 */
operator fun LcdInterface<*>.plusAssign(char: Char) {
    addText(char)
}

/**
 * Add a [special] code at current location
 */
operator fun LcdInterface<*>.plusAssign(special: Int) {
    addText(special)
}

/**
 * Run a motor at this relative speed (-1 to 1).
 */
infix fun MotorInterface.at(speed: Float) {
    if (speed < 0) {
        backward(abs(speed))
    } else {
        forward(speed)
    }
}
