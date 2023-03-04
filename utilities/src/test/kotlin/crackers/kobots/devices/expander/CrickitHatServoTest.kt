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

import com.diozero.api.ServoTrim
import crackers.kobots.devices.at
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
            testHat.pwmOutputPins = PWM_PINS
        }
        (1..4).forEach { servoNumber ->
            context("Servo $servoNumber:") {
                val servoPin = SERVOS[servoNumber - 1]
                val servoIndex = PWM_PINS.indexOf(servoPin).toByte() // because default Pi for testing
                val factory = CRICKITHatDeviceFactory(testHat)

                test("Via builder") {
                    factory.servo(servoNumber, ServoTrim.TOWERPRO_SG90).apply {
                        // test setup of default frequency of 50Hz
                        mockRequests shouldStartWith listOf(TIMER_BASE, TIMER_FREQ, servoIndex, 0x00, 0x32)

                        // clear the request buffer and move it
                        mockRequests.clear()
                        angle = 90f
                        angle = 180f
                        this at 0f // dsl
                    }
                    // N.B. make sure it's byte to byte, otherwise it tries to compare ints (and one is negative)
                    mockRequests shouldContainExactly
                        listOf(TIMER_BASE, TIMER_PWM, servoIndex, 0x12, 0x8F.toByte()) +
                        listOf(TIMER_BASE, TIMER_PWM, servoIndex, 0x1E, 0xB8.toByte()) +
                        listOf(TIMER_BASE, TIMER_PWM, servoIndex, 0x06, 0x66)
                }
            }
        }
    }
) {
    init {
        initProperties()
    }
}
