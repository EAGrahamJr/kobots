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

import com.diozero.api.ServoDevice
import com.diozero.api.ServoTrim
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.TIMER_BASE
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.TIMER_FREQ
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.TIMER_PWM
import crackers.kobots.devices.expander.CRICKITHat.Companion.PWM_PINS
import crackers.kobots.devices.expander.CRICKITHat.Companion.SERVOS
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldStartWith
import crackers.kobots.devices.MockI2CDevice.requests as mockRequests

/**
 * Schwing!
 */
class CrickitHatServoTest : FunSpec(
    {
        clearBeforeTest()
        beforeTest {
            testHat.seeSaw.pwmOutputPins = PWM_PINS
        }
        (1..4).forEach { servoNumber ->
            val servoPin = SERVOS[servoNumber - 1]
            val servoIndex = PWM_PINS.indexOf(servoPin).toByte()    // because default Pi for testing

            context("Servo $servoNumber:") {
                test("Set frequency") {
                    // default frequency is set on the constructor above
                    testHat.servo(servoNumber)
                    mockRequests shouldStartWith listOf(TIMER_BASE, TIMER_FREQ, servoIndex, 0x00, 0x32)
                }
            }
            context("CRICKIT servo $servoNumber:") {
                val device = testHat.servo(servoNumber, trim = ServoTrim.TOWERPRO_SG90)

                test("3 angles") {
                    device.run {
                        angle = 90
                        angle = 180
                        angle = 0
                    }
                    // N.B. make sure it's byte to byte, otherwise it tries to compare ints (and one is negative)
                    mockRequests shouldContainExactly
                            listOf(TIMER_BASE, TIMER_PWM, servoIndex, 0x12, 0x8F.toByte()) +
                            listOf(TIMER_BASE, TIMER_PWM, servoIndex, 0x1E, 0xB8.toByte()) +
                            listOf(TIMER_BASE, TIMER_PWM, servoIndex, 0x06, 0x66)
                }
            }
            context("diozero for $servoNumber:") {
                val id = CRICKITHatDeviceFactory.Types.SERVO.deviceNumber(servoNumber)
                val crickitHatDeviceFactory = CRICKITHatDeviceFactory(testHat)

                test("Get and use") {
                    ServoDevice.Builder.builder(id)
                        .setDeviceFactory(crickitHatDeviceFactory)
                        .setTrim(ServoTrim.TOWERPRO_SG90)
                        .build().apply {
                            mockRequests.clear()
                            angle = 90f
                            angle = 180f
                            angle = 0f
                        }
                    // N.B. make sure it's byte to byte, otherwise it tries to compare ints (and one is negative)
                    mockRequests shouldContainExactly
                            listOf(TIMER_BASE, TIMER_PWM, servoIndex, 0x12, 0x8F.toByte()) +
                            listOf(TIMER_BASE, TIMER_PWM, servoIndex, 0x1E, 0xB8.toByte()) +
                            listOf(TIMER_BASE, TIMER_PWM, servoIndex, 0x06, 0x66)
                }
            }
        }
    }) {
    init {
        initProperties()
    }
}
