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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.math.abs

class ActionSequenceTest : FunSpec(
    {
        /**
         * Test a sequence of a single action where the mock rotator is rotated to 90 degrees
         */
        test("simple rotate") {
            val rotator = MockRotator()
            val testSequence = sequence {
                action {
                    rotator.rotate {
                        angle = 90
                    }
                }
            }

            val actionDef = testSequence.build().first()
            val action = actionDef.action

            runAndGetCount(action::step) shouldBe 89
            action.step() shouldBe false
            rotator.current() shouldBe 90
        }

        /**
         * Test a sequence with two actions: move the mock rotator to a random angle, then back to 0; repeat the
         * sequence
         */
        test("re-runnable") {
            val rotator = MockRotator()
            val randomAngle = randomServoAngle()
            val testSequence = sequence {
                action {
                    rotator.rotate {
                        angle = randomAngle
                    }
                }
                action {
                    rotator.rotate {
                        angle = 0
                    }
                }
            }

            val listOfActions = testSequence.build()
            for (i in 1..2) {
                val firstActionDef = listOfActions.first()
                val secondActionDef = listOfActions.last()

                val firstAction = firstActionDef.action
                val secondAction = secondActionDef.action

                // N.B. moving to an absolute angle takes 1 less step than the angle
                val numberOfMoves = abs(randomAngle.toInt()) * 2 - 2
                runAndGetCount(firstAction::step) + runAndGetCount(secondAction::step) shouldBe numberOfMoves
                firstAction.step() shouldBe false
                secondAction.step() shouldBe false
                rotator.current() shouldBe 0f
            }
        }

        /**
         * Test a sequence of a single action where a mock rotator moves forward until a stop condition of 23 degrees
         * is reached
         */
        test("rotate forward") {
            val rotator = MockRotator()
            val testSequence = sequence {
                action {
                    rotator.forwardUntil { rotator.current() >= 23 }
                }
            }

            val actionDef = testSequence.build().first()
            val action = actionDef.action

            runAndGetCount(action::step) shouldBe 23
        }

        /**
         * Test two rotators moving in a single action, one moving to 45 degrees and the other moving backwards until
         * the fist one reaches 15 degrees
         */
        test("rotate forward and backward") {
            val rotator1 = MockRotator()
            val rotator2 = MockRotator()
            val testSequence = sequence {
                action {
                    rotator1.rotate {
                        angle = 45
                    }
                    rotator2.backwardUntil { rotator1.current() > 15 }
                }
            }

            val actionDef = testSequence.build().first()
            val action = actionDef.action

            // N.B. moving to an absolute angle takes 1 less step than the angle
            runAndGetCount(action::step) shouldBe 44
            rotator1.current() shouldBe 45
            rotator2.current() shouldBe -15f
        }

        /**
         * Test a sequence of a single action where three mock rotators each move to a random angle,
         * with the number of action steps invoked equal to one less than the largest angle moved
         */
        test("rotate random") {
            val rotator1 = MockRotator()
            val rotator2 = MockRotator()
            val rotator3 = MockRotator()
            val angle1 = randomServoAngle()
            val angle2 = randomServoAngle()
            val angle3 = randomServoAngle()
            // N.B. moving to an absolute angle takes 1 less step than the angle
            val maxSteps = listOf(angle1, angle2, angle3).max().toInt() - 1

            val testSequence = sequence {
                action {
                    rotator1.rotate {
                        angle = angle1
                    }
                    rotator2.rotate {
                        angle = angle2
                    }
                    rotator3.rotate {
                        angle = angle3
                    }
                }
            }

            val actionDef = testSequence.build().first()
            val action = actionDef.action

            runAndGetCount(action::step) shouldBe maxSteps
            rotator1.current() shouldBe angle1
            rotator2.current() shouldBe angle2
            rotator3.current() shouldBe angle3
        }

        /**
         * Test a sequence of two actions where the mock rotator is rotated to random angles and the initial position
         * of the rotator for the second action is checked against the desired results of the first action; the final
         * position of the rotator should be that of the second action
         */
        test("multiple actions") {
            val rotator = MockRotator()
            val angles = listOf(randomServoAngle(), randomServoAngle())
            var secondStartsAt: Int? = null

            val testSequence = sequence {
                action {
                    rotator.rotate {
                        angle = angles[0]
                    }
                }
                action {
                    rotator.rotate {
                        angle = angles[1]
                        stopCheck = {
                            if (secondStartsAt == null) secondStartsAt = rotator.current()
                            false
                        }
                    }
                }
            }

            var count = 0
            testSequence.build().forEach { actionDef ->
                val action = actionDef.action
                count += runAndGetCount(action::step)
            }
            secondStartsAt shouldBe angles[0]
            rotator.current() shouldBe angles[1]

            // the count should bhe the number of steps to get to the first angle (minus 1) plus the number of steps to
            // get to the second angle from the first (minus 1)
            count shouldBe (angles[0] + abs(angles[1] - angles[0])) - 2
        }

        /**
         * Test running a  sequence  5 times; the sequence contains a single action where the mock rotator is
         * rotated to a random angle; each rotation is checked to ensure that the random angle is different each time
         */
        test("repeated with external influences") {
            val rotator = MockRotator()
            val angles = listOf(
                randomServoAngle(),
                randomServoAngle(),
                randomServoAngle(),
                randomServoAngle(),
                randomServoAngle()
            )
            val stoppedAt = mutableListOf<Int>()

            var angleIndex = -1 // the external influence on the sequence

            val testSequence = sequence {
                action {
                    rotator.forwardUntil {
                        rotator.current().let { current ->
                            (current == angles[angleIndex]).also { hasStopped ->
                                if (hasStopped) stoppedAt += current
                            }
                        }
                    }
                }
                action {
                    rotator.rotate { angle = 0 }
                }
            }

            val actionDef = testSequence.build().first()
            val resetDef = testSequence.build().last()

            for (i in 0 until 5) {
                angleIndex = i
                val randomAction = actionDef.action
                val resetAction = resetDef.action

                val count = runAndGetCount(randomAction::step) + runAndGetCount(resetAction::step)
                count shouldBe abs(angles[i]) * 2 - 1 // -1 for the home move
                rotator.current() shouldBe 0
                stoppedAt[i] shouldBe angles[i]
            }
        }

        /**
         * Test a sequence of a single action that uses the `execute` function to run a lambda that returns true the
         * 5th time it is invoked. The action should take 5 steps to complete.
         */
        test("code execution as an action step") {
            var counter = 0

            val testSequence = sequence {
                action {
                    execute {
                        if (counter == 5) {
                            true
                        } else {
                            ++counter
                            false
                        }
                    }
                }
            }

            val actionDef = testSequence.build().first()
            val action = actionDef.action

            runAndGetCount(action::step) shouldBe 5
        }

        /**
         * Test a sequence of one action with two `execute` functions, each of which increments a counter: one returns true
         * when the counter is 5, the other returns true when the counter is 10. The action should take 10 steps to complete.
         */
        test("multiple code executions as action steps") {
            var counter1 = 0
            var counter2 = 0

            val testSequence = sequence {
                action {
                    execute {
                        ++counter1 > 5
                    }
                    execute {
                        ++counter2 > 10
                    }
                }
            }

            val actionDef = testSequence.build().first()
            val action = actionDef.action

            runAndGetCount(action::step) shouldBe 10
        }
    }
)
