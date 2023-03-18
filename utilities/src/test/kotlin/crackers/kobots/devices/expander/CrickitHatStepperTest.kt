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

import com.diozero.devices.sandpit.motor.BasicStepperMotor
import com.diozero.devices.sandpit.motor.StepperMotorInterface
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.TIMER_BASE
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.TIMER_FREQ
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
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

        context("Driver Setups:") {
            test("Unipolar driver") {
                val pinOrder = listOf(0x0b, 0x0a, 0x09, 0x08).map { it.toByte() }

                factory.unipolarStepperPort().use {
                    checkStepperSetup(pinOrder)
                }
            }
            test("Bipolar driver") {
                val pinOrder = listOf(0x07, 0x06, 0x05, 0x04).map { it.toByte() }

                factory.motorStepperPort().use {
                    checkStepperSetup(pinOrder)
                }
            }
        }
        context("Step forward and backward one cycle:") {
            test("Unipolar driver") {
                factory.unipolarStepperPort().use { driver ->
                    val stepper = BasicStepperMotor(512, driver)
                    mockRequests.clear()

                    (1..4).forEach { stepper.step(StepperMotorInterface.Direction.FORWARD) }
                    (1..4).forEach { stepper.step(StepperMotorInterface.Direction.BACKWARD) }

                    val expected = listOf(
                        0x08, 0x01, 0x0b, 0x00, 0x00,
                        0x08, 0x01, 0x0a, 0x00, 0x00,
                        0x08, 0x01, 0x09, 0x00, 0x00,
                        0x08, 0x01, 0x08, 0xff, 0xff,

                        0x08, 0x01, 0x0b, 0x00, 0x00,
                        0x08, 0x01, 0x0a, 0x00, 0x00,
                        0x08, 0x01, 0x09, 0xff, 0xff,
                        0x08, 0x01, 0x08, 0x00, 0x00,

                        0x08, 0x01, 0x0b, 0x00, 0x00,
                        0x08, 0x01, 0x0a, 0xff, 0xff,
                        0x08, 0x01, 0x09, 0x00, 0x00,
                        0x08, 0x01, 0x08, 0x00, 0x00,

                        0x08, 0x01, 0x0b, 0xff, 0xff,
                        0x08, 0x01, 0x0a, 0x00, 0x00,
                        0x08, 0x01, 0x09, 0x00, 0x00,
                        0x08, 0x01, 0x08, 0x00, 0x00,

                        0x08, 0x01, 0x0b, 0x00, 0x00,
                        0x08, 0x01, 0x0a, 0xff, 0xff,
                        0x08, 0x01, 0x09, 0x00, 0x00,
                        0x08, 0x01, 0x08, 0x00, 0x00,

                        0x08, 0x01, 0x0b, 0x00, 0x00,
                        0x08, 0x01, 0x0a, 0x00, 0x00,
                        0x08, 0x01, 0x09, 0xff, 0xff,
                        0x08, 0x01, 0x08, 0x00, 0x00,

                        0x08, 0x01, 0x0b, 0x00, 0x00,
                        0x08, 0x01, 0x0a, 0x00, 0x00,
                        0x08, 0x01, 0x09, 0x00, 0x00,
                        0x08, 0x01, 0x08, 0xff, 0xff,

                        0x08, 0x01, 0x0b, 0xff, 0xff,
                        0x08, 0x01, 0x0a, 0x00, 0x00,
                        0x08, 0x01, 0x09, 0x00, 0x00,
                        0x08, 0x01, 0x08, 0x00, 0x00
                    ).map { it.toByte() }

                    mockRequests shouldContainExactly expected
                }
            }
            test("Bipolar driver") {
                factory.motorStepperPort().use { driver ->
                    val stepper = BasicStepperMotor(driver)
                    mockRequests.clear()

                    (1..4).forEach { stepper.step(StepperMotorInterface.Direction.FORWARD) }
                    (1..4).forEach { stepper.step(StepperMotorInterface.Direction.BACKWARD) }

                    val expected = listOf(
                        0x08, 0x01, 0x07, 0x00, 0x00,
                        0x08, 0x01, 0x05, 0x00, 0x00,
                        0x08, 0x01, 0x06, 0x00, 0x00,
                        0x08, 0x01, 0x04, 0xff, 0xff,

                        0x08, 0x01, 0x07, 0x00, 0x00,
                        0x08, 0x01, 0x05, 0x00, 0x00,
                        0x08, 0x01, 0x06, 0xff, 0xff,
                        0x08, 0x01, 0x04, 0x00, 0x00,

                        0x08, 0x01, 0x07, 0x00, 0x00,
                        0x08, 0x01, 0x05, 0xff, 0xff,
                        0x08, 0x01, 0x06, 0x00, 0x00,
                        0x08, 0x01, 0x04, 0x00, 0x00,

                        0x08, 0x01, 0x07, 0xff, 0xff,
                        0x08, 0x01, 0x05, 0x00, 0x00,
                        0x08, 0x01, 0x06, 0x00, 0x00,
                        0x08, 0x01, 0x04, 0x00, 0x00,

                        0x08, 0x01, 0x07, 0x00, 0x00,
                        0x08, 0x01, 0x05, 0xff, 0xff,
                        0x08, 0x01, 0x06, 0x00, 0x00,
                        0x08, 0x01, 0x04, 0x00, 0x00,

                        0x08, 0x01, 0x07, 0x00, 0x00,
                        0x08, 0x01, 0x05, 0x00, 0x00,
                        0x08, 0x01, 0x06, 0xff, 0xff,
                        0x08, 0x01, 0x04, 0x00, 0x00,

                        0x08, 0x01, 0x07, 0x00, 0x00,
                        0x08, 0x01, 0x05, 0x00, 0x00,
                        0x08, 0x01, 0x06, 0x00, 0x00,
                        0x08, 0x01, 0x04, 0xff, 0xff,

                        0x08, 0x01, 0x07, 0xff, 0xff,
                        0x08, 0x01, 0x05, 0x00, 0x00,
                        0x08, 0x01, 0x06, 0x00, 0x00,
                        0x08, 0x01, 0x04, 0x00, 0x00
                    ).map { it.toByte() }

                    mockRequests shouldContainExactly expected
                }
            }
        }
    }
) {

    init {
        initProperties()
    }
}


private fun checkStepperSetup(pinOrder: List<Byte>) {
    mockRequests shouldContainExactly listOf(
        TIMER_BASE, TIMER_FREQ, pinOrder[0], 0x07, 0xd0.toByte(),
        TIMER_BASE, TIMER_FREQ, pinOrder[1], 0x07, 0xd0.toByte(),
        TIMER_BASE, TIMER_FREQ, pinOrder[2], 0x07, 0xd0.toByte(),
        TIMER_BASE, TIMER_FREQ, pinOrder[3], 0x07, 0xd0.toByte()
    )
}
