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

package crackers.kobots.parts

import com.diozero.api.ServoDevice
import com.diozero.devices.sandpit.motor.StepperMotorInterface
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.verify

/**
 * Testing the rotator classes.
 */
class RotatorTest : FunSpec(
    {
        @MockK
        lateinit var mockStepper: StepperMotorInterface

        @MockK
        lateinit var mockServo: ServoDevice

        beforeTest {
            mockStepper = mockk<StepperMotorInterface>()
            mockServo = mockkClass(ServoDevice::class)
        }

        /**
         * Test the rotatable with a stepper motor: the gear ratio is 1:1 and the stepper motor has 200 steps per rotation.
         * The rotatable is set to move from 0 to 360 degrees, and it's starting position is assumed to be 9 degrees.
         * The target angle is 83 degrees.
         */
        test("rotatable with stepper") {
            every { mockStepper.stepsPerRotation } answers { 200 }
            every { mockStepper.step(any()) } answers { }
            val rotatable = BasicStepperRotator(mockStepper)

            while (!rotatable.rotateTo(83)) {
                // just count things
            }
            verify(exactly = 46) {
                mockStepper.step(StepperMotorInterface.Direction.FORWARD)
            }
        }

        /**
         * Test a stepper rotator with a gear ratio of 1:1.11 and the motor has 200 steps per rotation. The rotator is
         * to be moved 90 degrees in a forward direction. The stepper is currently at 0 degrees.
         */
        test("rotatable with stepper and gear ratio") {
            every { mockStepper.stepsPerRotation } answers { 200 }
            every { mockStepper.step(any()) } answers { }
            val rotatable = BasicStepperRotator(mockStepper, 1.11f)

            while (!rotatable.rotateTo(90)) {
                // just count things
            }
            verify(exactly = 45) {
                mockStepper.step(StepperMotorInterface.Direction.FORWARD)
            }
        }

        /**
         * Test a rotatable with a servo motor. The physical range is 0 to 90 degrees and the servo has a range of 0 to 180.
         * Assume the servo angle is at 43 degrees and the target is 87 physical degrees.
         */
        test("rotatable with servo") {
            var currentAngle = 43f
            every { mockServo.angle } answers { currentAngle }
            every { mockServo.setAngle(any()) } answers {
                currentAngle = args[0] as Float
            }
            val rotatable = ServoRotator(mockServo, IntRange(0, 90), IntRange(0, 180))

            while (!rotatable.rotateTo(87)) {
                // just count things
            }
            verify(exactly = 131) {
                mockServo.setAngle(any())
            }
            currentAngle shouldBe 174f
        }

        /**
         * Test a rotatable with a servo motor. The physical range is -10 to 90 degrees and the servo has a range of
         * 180 to 0. Assume the servo angle is at 162 degrees and the target is 45 physical degrees.
         */
        test("rotatable with servo reversed") {
            var currentAngle = 162f
            every { mockServo.angle } answers { currentAngle }
            every { mockServo.setAngle(any()) } answers {
                currentAngle = args[0] as Float
            }
            val rotatable = ServoRotator(mockServo, IntRange(-10, 90), IntRange(180, 0))

            while (!rotatable.rotateTo(45)) {
                // just count things
            }
            verify(exactly = 81) {
                mockServo.setAngle(any())
            }
            currentAngle shouldBe 81f
        }

        /**
         * Test a rotatable with a servo motor. The physical range is 0 to 90 and the servo has a range of 0 to 180.
         * The delta is 5 degrees. The servo is currently at an angle of 60 degrees and the target is 32 degrees.
         * Verify the servo does not move.
         */
        test("rotatable with servo delta") {
            var currentAngle = 60f
            every { mockServo.angle } answers { currentAngle }
            every { mockServo.setAngle(any()) } answers {
                currentAngle = args[0] as Float
            }
            val rotatable = ServoRotator(mockServo, IntRange(0, 90), IntRange(0, 180), 5)

            while (!rotatable.rotateTo(32)) {
                // just count things
            }
            verify(exactly = 0) {
                mockServo.setAngle(any())
            }
            currentAngle shouldBe 60f
        }
    }
)
