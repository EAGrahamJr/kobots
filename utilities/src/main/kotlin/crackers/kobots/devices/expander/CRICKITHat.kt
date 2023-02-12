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

package crackers.kobots.devices.expander

import com.diozero.api.I2CDevice
import crackers.kobots.devices.expander.CRICKITHat.Companion.DEVICE_ADDRESS

/**
 * A CRICKIT Hat.
 */
internal val defaultI2CDevice by lazy { I2CDevice(1, DEVICE_ADDRESS) }

class CRICKITHat(i2CDevice: I2CDevice = defaultI2CDevice, initReset: Boolean = true) : AutoCloseable {
    val seeSaw = AdafruitSeeSaw(i2CDevice, initReset = initReset).apply {
        analogInputPins = DIGITAL_PINS
        pwmOutputPins = PWM_PINS
    }

    override fun close() {
        try {
            seeSaw.close()
        } catch (_: Throwable) {
        }
    }

    /**
     * Get a device for the indicated touchpad (1-4)
     */
    internal fun touch(index: Int) = CRICKITTouch(seeSaw, index)

    /**
     * Get a device for the indicated signal (1-8)
     */
    internal fun signal(index: Int) = DIGITAL_PINS[index - 1].let { pin -> CRICKITSignal(seeSaw, pin) }

    companion object {
        const val DEVICE_ADDRESS = 0x49

        const val ANALOG_MAX = 1023f

        const val CAPTOUCH1 = 4
        const val CAPTOUCH2 = 5
        const val CAPTOUCH3 = 6
        const val CAPTOUCH4 = 7

        /**
         * SeeSaw pins in order for the touchpads
         */
        val TOUCH_PAD_PINS = intArrayOf(CAPTOUCH1, CAPTOUCH2, CAPTOUCH3, CAPTOUCH4)

        const val SIGNAL1 = 2
        const val SIGNAL2 = 3
        const val SIGNAL3 = 40
        const val SIGNAL4 = 41
        const val SIGNAL5 = 11
        const val SIGNAL6 = 10
        const val SIGNAL7 = 9
        const val SIGNAL8 = 8

        /**
         * SeeSaw pins in order for the signal ports
         */
        val DIGITAL_PINS = intArrayOf(SIGNAL1, SIGNAL2, SIGNAL3, SIGNAL4, SIGNAL5, SIGNAL6, SIGNAL7, SIGNAL8)

        const val SERVO4 = 14
        const val SERVO3 = 15
        const val SERVO2 = 16
        const val SERVO1 = 17

        /**
         * Logical order - **NOT** pin order (see [PWM_PINS]
         */
        val SERVOS = intArrayOf(SERVO1, SERVO2, SERVO3, SERVO4)

        const val MOTOR2B = 18
        const val MOTOR2A = 19
        const val MOTOR1A = 22
        const val MOTOR1B = 23

        /**
         * Logical order - **NOT** pin order (see [PWM_PINS]
         */
        val MOTORS = arrayOf(MOTOR1A to MOTOR1B, MOTOR2A to MOTOR2B)

        const val DRIVE4 = 42
        const val DRIVE3 = 43
        const val DRIVE2 = 12
        const val DRIVE1 = 13

        /**
         * Logical order - **NOT** pin order (see [PWM_PINS]
         */
        val DRIVES = intArrayOf(DRIVE1, DRIVE2, DRIVE3, DRIVE4)

        /**
         * SeeSaw pins in order for the analog outputs
         */
        val PWM_PINS =
            intArrayOf(
                SERVO4, SERVO3, SERVO2, SERVO1,
                MOTOR2B, MOTOR2A, MOTOR1B, MOTOR1A,
                DRIVE4, DRIVE3, DRIVE2, DRIVE1
            )
    }
}
