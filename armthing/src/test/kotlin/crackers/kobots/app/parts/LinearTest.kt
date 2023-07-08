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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.verify
import kotlin.math.roundToInt

/**
 * Tests the linear actuators.
 */
class LinearTest : FunSpec(
    {
        /**
         * Test the servo linear actuator with the max angle set to 180 and the minimum angle set to 0, moves about 100
         * degrees when the actuator is moved to 60 percent
         */
        test("servo linear actuator with stepper") {
            val mockServo: ServoDevice = mockkClass(ServoDevice::class)
            val linear = ServoLinearActuator(mockServo, 0f, 180f)
            var currentServoAngle = 0f
            every { mockServo.angle } answers { currentServoAngle }
            every { mockServo.angle = any() } answers { currentServoAngle = arg(0) }

            while (!linear.extendTo(60)) {
                // just count things
            }
            verify(atLeast = (.6 * 180).roundToInt()) {
                mockServo.angle = any()
            }
        }

        /**
         * Test a linear actuator with a servo motor with the home angle set to 100 degrees and the max angle set
         * to 0 degrees. The servo is at 75 degrees and test that moving to the 0% position moves the motor the
         * appropriate number of steps.
         */
        test("servo linear actuator with stepper and home angle") {
            val mockServo: ServoDevice = mockkClass(ServoDevice::class)
            val linear = ServoLinearActuator(mockServo, 100f, 0f)
            var currentServoAngle = 75f
            every { mockServo.angle } answers { currentServoAngle }
            every { mockServo.angle = any() } answers { currentServoAngle = arg(0) }

            while (!linear.extendTo(0)) {
                // just count things
            }
            verify(atLeast = 25) {
                mockServo.angle = any()
            }
            currentServoAngle shouldBe 100f
        }
    }
)
