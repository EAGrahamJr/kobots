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

package crackers.kobots.app.parts

import com.diozero.api.ServoDevice
import com.diozero.devices.sandpit.motor.StepperMotorInterface
import crackers.kobots.app.arm.RotatableServo
import crackers.kobots.app.arm.RotatableStepper
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.verify
import kotlin.math.roundToInt

/**
 * Testing the rotator classes.
 */
class RotatableTest : FunSpec(
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
         * The rotatable is set to move from 0 to 360 degrees and it's starting position is assumbed to be 9 degrees.
         * The target angle is 83 degrees.
         */
        test("rotatable with stepper") {
            every { mockStepper.getStepsPerRotation() } answers { 200 }
            every { mockStepper.step(any()) } answers { }
            val rotatable = RotatableStepper(mockStepper)

            while (!rotatable.moveTowards(83f)) {
                // just count things
            }
            verify(atLeast = (74f * 200 / 360).roundToInt()) {
                mockStepper.step(StepperMotorInterface.Direction.FORWARD)
            }
        }

        /**
         * Test a rotatable servo motor: the servo starts at 42 degrees and the home position is 105 with the maximum
         * at 3 degrees. The rotatable has a delta of 1 degree and the target is 90 degrees.
         */
        test("rotatable with servo") {
            val rotatable = RotatableServo(mockServo, 105f, 3f, 1f)
            var currentServoAngle = 42f
            every { mockServo.angle } answers { currentServoAngle }
            every { mockServo.angle = any() } answers { currentServoAngle = arg(0) }

            while (!rotatable.moveTowards(90f)) {
                // just count things
            }
            verify(atLeast = 90 - 42) {
                mockServo.angle = any()
            }
        }

        /**
         * Test attempting to move a rotabable servo to an angle greater than the maximum the servo supports. The
         * servo home position is 0 degrees and the maximum is 90 degrees. The rotatable has a delta of 1 degree and
         * the target is 100 degrees.
         */
        test("rotatable with servo - beyond maximum") {
            val rotatable = RotatableServo(mockServo, 0f, 90f, 1f)
            var currentServoAngle = 0f
            every { mockServo.angle } answers { currentServoAngle }
            every { mockServo.angle = any() } answers { currentServoAngle = arg(0) }

            while (!rotatable.moveTowards(100f)) {
                // just count things
            }
            // one less because we hit maximum already
            verify(atLeast = 89) {
                mockServo.angle = any()
            }
        }
        /**
         * TODO test servo with arc defined
         */
        /**
         * TODO test stepper with arc defined and negative direction
         */
    })
