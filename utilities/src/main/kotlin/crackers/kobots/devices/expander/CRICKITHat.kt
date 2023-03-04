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

/**
 * CRICKIT Hat pin definitions for the SeeSaw device.
 */
class CRICKITHat(i2CDevice: I2CDevice = defaultI2CDevice, initReset: Boolean = true) :
    AdafruitSeeSaw(i2CDevice, initReset) {
    init {
        analogInputPins = DIGITAL_PINS
        pwmOutputPins = PWM_PINS
    }

    companion object {
        internal const val DEVICE_ADDRESS = 0x49
        internal val defaultI2CDevice by lazy { I2CDevice(1, DEVICE_ADDRESS) }

        internal const val ANALOG_MAX = 1023f

        internal const val CAPTOUCH1 = 4
        internal const val CAPTOUCH2 = 5
        internal const val CAPTOUCH3 = 6
        internal const val CAPTOUCH4 = 7

        /**
         * SeeSaw pins in order for the touchpads
         */
        internal val TOUCH_PAD_PINS = intArrayOf(CAPTOUCH1, CAPTOUCH2, CAPTOUCH3, CAPTOUCH4)

        internal const val SIGNAL1 = 2
        internal const val SIGNAL2 = 3
        internal const val SIGNAL3 = 40
        internal const val SIGNAL4 = 41
        internal const val SIGNAL5 = 11
        internal const val SIGNAL6 = 10
        internal const val SIGNAL7 = 9
        internal const val SIGNAL8 = 8

        /**
         * SeeSaw pins in order for the signal ports
         */
        internal val DIGITAL_PINS = intArrayOf(SIGNAL1, SIGNAL2, SIGNAL3, SIGNAL4, SIGNAL5, SIGNAL6, SIGNAL7, SIGNAL8)

        internal const val SERVO4 = 14
        internal const val SERVO3 = 15
        internal const val SERVO2 = 16
        internal const val SERVO1 = 17

        /**
         * Logical order - **NOT** pin order (see [PWM_PINS]
         */
        internal val SERVOS = intArrayOf(SERVO1, SERVO2, SERVO3, SERVO4)

        internal const val MOTOR2B = 18
        internal const val MOTOR2A = 19
        internal const val MOTOR1A = 22
        internal const val MOTOR1B = 23

        /**
         * Logical order - **NOT** pin order (see [PWM_PINS]
         */
        internal val MOTORS = intArrayOf(MOTOR1A, MOTOR1B, MOTOR2A, MOTOR2B)

        internal const val DRIVE4 = 42
        internal const val DRIVE3 = 43
        internal const val DRIVE2 = 12
        internal const val DRIVE1 = 13

        /**
         * Logical order - **NOT** pin order (see [PWM_PINS]
         */
        internal val DRIVES = intArrayOf(DRIVE1, DRIVE2, DRIVE3, DRIVE4)

        /**
         * SeeSaw pins in order for the analog outputs
         */
        internal val PWM_PINS =
            intArrayOf(
                SERVO4, SERVO3, SERVO2, SERVO1,
                MOTOR2B, MOTOR2A, MOTOR1B, MOTOR1A,
                DRIVE4, DRIVE3, DRIVE2, DRIVE1
            )
    }
}
