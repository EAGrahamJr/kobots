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

import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_BASE
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_BULK_CLEAR
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_BULK_SET
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_DIRECTION_INPUT
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_DIRECTION_OUTPUT
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_PULL_DISABLED
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_PULL_ENABLED
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.SignalMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import crackers.kobots.devices.MockI2CDevice.requests as mockRequests

/**
 * Test data values were captured from the Adafruit Circuit Python code directly
 */
class CrickitHatSignalsTest : FunSpec(
    {
        clearBeforeTest()

        (1..8).forEach { signal ->
            val pinSelectorBytes = signalPinSelectors[signal - 1]
            context("Digital signal setup [$signal]") {
                test("Output") {
                    val directionOutputSetup = listOf(GPIO_BASE, GPIO_DIRECTION_OUTPUT)
                    val setupCommand = directionOutputSetup + pinSelectorBytes

                    testHat.signal(signal).mode = SignalMode.OUTPUT
                    mockRequests.shouldContainExactly(setupCommand)
                }
                test("Input") {
                    val directionOutputSetup = listOf(GPIO_BASE, GPIO_DIRECTION_INPUT)
                    val disablePullResistor = listOf(GPIO_BASE, GPIO_PULL_DISABLED)
                    val setupCommand =
                        directionOutputSetup + pinSelectorBytes +
                                disablePullResistor + pinSelectorBytes

                    testHat.signal(signal).mode = SignalMode.INPUT
                    mockRequests.shouldContainExactly(setupCommand)
                }
                test("Input with pull-up") {
                    val directionOutputSetup = listOf(GPIO_BASE, GPIO_DIRECTION_INPUT)
                    val enablePullResistor = listOf(GPIO_BASE, GPIO_PULL_ENABLED)
                    val bulkSet = listOf(GPIO_BASE, GPIO_BULK_SET)

                    val setupCommand =
                        directionOutputSetup + pinSelectorBytes +
                                enablePullResistor + pinSelectorBytes +
                                bulkSet + pinSelectorBytes

                    testHat.signal(signal).mode = SignalMode.INPUT_PULLUP
                    mockRequests.shouldContainExactly(setupCommand)
                }
                test("Input with pull-down") {
                    val directionOutputSetup = listOf(GPIO_BASE, GPIO_DIRECTION_INPUT)
                    val enablePullResistor = listOf(GPIO_BASE, GPIO_PULL_ENABLED)
                    val bulkClear = listOf(GPIO_BASE, GPIO_BULK_CLEAR)

                    val setupCommand =
                        directionOutputSetup + pinSelectorBytes +
                                enablePullResistor + pinSelectorBytes +
                                bulkClear + pinSelectorBytes

                    testHat.signal(signal).mode = SignalMode.INPUT_PULLDOWN
                    mockRequests.shouldContainExactly(setupCommand)
                }
            }

            context("Digital output [$signal]") {
                test("Write to output") {
                }
                test("Write to input (and fail)") {
                }
            }

            context("Digital input [$signal]") {
                test("Read from input") {
                }
                test("Read from output (and fail)") {
                }
            }
        }
    }
) {
    init {
        initProperties()
    }
}

val signalPinSelectors = arrayOf<List<Byte>>(
    listOf(0x00, 0x00, 0x00, 0x04),
    listOf(0x00, 0x00, 0x00, 0x08),
    listOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00),
    listOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00),
    listOf(0x00, 0x00, 0x08, 0x00),
    listOf(0x00, 0x00, 0x04, 0x00),
    listOf(0x00, 0x00, 0x02, 0x00),
    listOf(0x00, 0x00, 0x01, 0x00)
)
