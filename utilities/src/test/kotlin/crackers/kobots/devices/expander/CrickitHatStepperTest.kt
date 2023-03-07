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

import com.diozero.devices.sandpit.motor.BYJ48Stepper
import com.diozero.devices.sandpit.motor.StepperMotorInterface
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.TIMER_BASE
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.TIMER_FREQ
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.assertEquals
import crackers.kobots.devices.MockI2CDevice.requests as mockRequests

/**
 * Test the stepper stuff.
 */
class CrickitHatStepperTest : FunSpec(
    {
        clearBeforeTest()
        beforeTest {
            testHat.pwmOutputPins = CRICKITHat.PWM_PINS
        }
        val factory = CRICKITHatDeviceFactory(testHat)

        val pinOrder = listOf(0x09, 0x0a, 0x0b, 0x08).map { it.toByte() }

        context("Driver Setups:") {

            test("Unipolar stepper") {
                factory.unipolarStepper(false).use {
                    mockRequests shouldContainExactly listOf(
                        // TODO this is the "2000" setting from the Adafruit python library - this is the "base" PWM
                        // TODO frequency in the Servo class (very confused)
                        TIMER_BASE, TIMER_FREQ, pinOrder[0], 0x07, 0xd0.toByte(),
                        TIMER_BASE, TIMER_FREQ, pinOrder[1], 0x07, 0xd0.toByte(),
                        TIMER_BASE, TIMER_FREQ, pinOrder[2], 0x07, 0xd0.toByte(),
                        TIMER_BASE, TIMER_FREQ, pinOrder[3], 0x07, 0xd0.toByte()
                    )
                }
            }
        }
        context("Unipolar driver:") {
            factory.unipolarStepper(false).use { driver ->
                test("Step forward and backward one") {
                    val stepper = BYJ48Stepper(driver)
                    mockRequests.clear()

                    stepper.step(StepperMotorInterface.Direction.FORWARD)
                    stepper.step(StepperMotorInterface.Direction.BACKWARD)

                    val expected = listOf(
                        0x08, 0x01, 0x09, 0x00, 0x00,
                        0x08, 0x01, 0x0a, 0x00, 0x00,
                        0x08, 0x01, 0x0b, 0x00, 0x00,
                        0x08, 0x01, 0x08, 0xff, 0xff,

                        0x08, 0x01, 0x09, 0x00, 0x00,
                        0x08, 0x01, 0x0a, 0x00, 0x00,
                        0x08, 0x01, 0x0b, 0xff, 0xff,
                        0x08, 0x01, 0x08, 0x00, 0x00,

                        0x08, 0x01, 0x09, 0x00, 0x00,
                        0x08, 0x01, 0x0a, 0xff, 0xff,
                        0x08, 0x01, 0x0b, 0x00, 0x00,
                        0x08, 0x01, 0x08, 0x00, 0x00,

                        0x08, 0x01, 0x09, 0xff, 0xff,
                        0x08, 0x01, 0x0a, 0x00, 0x00,
                        0x08, 0x01, 0x0b, 0x00, 0x00,
                        0x08, 0x01, 0x08, 0x00, 0x00,

                        0x08, 0x01, 0x09, 0x00, 0x00,
                        0x08, 0x01, 0x0a, 0xff, 0xff,
                        0x08, 0x01, 0x0b, 0x00, 0x00,
                        0x08, 0x01, 0x08, 0x00, 0x00,

                        0x08, 0x01, 0x09, 0x00, 0x00,
                        0x08, 0x01, 0x0a, 0x00, 0x00,
                        0x08, 0x01, 0x0b, 0xff, 0xff,
                        0x08, 0x01, 0x08, 0x00, 0x00,

                        0x08, 0x01, 0x09, 0x00, 0x00,
                        0x08, 0x01, 0x0a, 0x00, 0x00,
                        0x08, 0x01, 0x0b, 0x00, 0x00,
                        0x08, 0x01, 0x08, 0xff, 0xff,

                        0x08, 0x01, 0x09, 0xff, 0xff,
                        0x08, 0x01, 0x0a, 0x00, 0x00,
                        0x08, 0x01, 0x0b, 0x00, 0x00,
                        0x08, 0x01, 0x08, 0x00, 0x00,
                    ).map { it.toByte() }

                    mockRequests.size shouldBe expected.size
                    mockRequests.forEachIndexed { index, byte ->
                        assertEquals(expected[index], byte, "Index $index does not match")
                    }
//                    mockRequests shouldContainExactly expected
                }
            }
        }
    })
