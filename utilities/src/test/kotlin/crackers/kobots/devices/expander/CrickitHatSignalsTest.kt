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

import com.diozero.api.InvalidModeException
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.ADC_BASE
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.ADC_CHANNEL_OFFSET
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_BASE
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_BULK
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_BULK_CLEAR
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_BULK_SET
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_DIRECTION_INPUT
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_DIRECTION_OUTPUT
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_PULL_DISABLED
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_PULL_ENABLED
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.SignalMode
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import crackers.kobots.devices.MockI2CDevice.requests as mockRequests
import crackers.kobots.devices.MockI2CDevice.responses as mockResponses

/**
 * Test data values were captured from the Adafruit Circuit Python code directly
 */
class CrickitHatSignalsTest : FunSpec(
    {
        clearBeforeTest()

        (1..8).forEach { signal ->
            val pinSelectorBytes = signalPinSelectors[signal - 1]
            context("Signal $signal setup") {
                test("Output") {
                    val directionOutputSetup = listOf(GPIO_BASE, GPIO_DIRECTION_OUTPUT)
                    val setupCommand = directionOutputSetup + pinSelectorBytes

                    testHat.signal(signal).mode = SignalMode.OUTPUT
                    mockRequests shouldContainExactly setupCommand
                }
                test("Input") {
                    val directionOutputSetup = listOf(GPIO_BASE, GPIO_DIRECTION_INPUT)
                    val disablePullResistor = listOf(GPIO_BASE, GPIO_PULL_DISABLED)
                    val setupCommand =
                        directionOutputSetup + pinSelectorBytes +
                                disablePullResistor + pinSelectorBytes

                    testHat.signal(signal).mode = SignalMode.INPUT
                    mockRequests shouldContainExactly setupCommand
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
                    mockRequests shouldContainExactly setupCommand
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
                    mockRequests shouldContainExactly setupCommand
                }
            }

            context("Signal $signal digital output:") {
                // setup output
                val output = testHat.signal(signal).also {
                    it.mode = SignalMode.OUTPUT
                    mockRequests.clear()
                }

                test("Write") {
                    val turnOnCommand = listOf(GPIO_BASE, GPIO_BULK_SET) + pinSelectorBytes
                    output.value = true
                    mockRequests shouldContainExactly turnOnCommand
                }
                test("Read (and fail)") {
                    shouldThrowWithMessage<InvalidModeException>("Input is not allowed on output devices.") {
                        output.value
                    }
                }
            }

            context("Signal $signal digital input:") {
                // setup input
                val input = testHat.signal(signal).apply {
                    mode = SignalMode.INPUT_PULLDOWN
                    mockRequests.clear()
                }
                val blockSize = pinSelectorBytes.size

                test("Read") {
                    // set the response to match the mask, ergo "true"
                    mockResponses.push(ByteArray(blockSize) { index -> pinSelectorBytes[index] })

                    val readCommand = listOf(GPIO_BASE, GPIO_BULK)
                    input.value shouldBe true
                    mockRequests shouldContainExactly readCommand
                }
                test("Write (and fail)") {
                    mockResponses.push(ByteArray(blockSize) { 0.toByte() })
                    shouldThrowWithMessage<InvalidModeException>("Output is not allowed on input devices.") {
                        input.value = true
                        null
                    }
                }
            }

            context("Signal $signal analog input:") {
                // TODO adafruit seems to not require setting a mode to read tha analog
                test("Read (SAM D09)") {
                    // setup input - the test hat is already initialized to mimic the Pi
                    val input = testHat.signal(signal).apply {
                        mode = SignalMode.INPUT
                        mockRequests.clear()
                    }

                    // data read
                    mockResponses.apply {
                        push(byteArrayOf(3, 0xFE.toByte()))
                        push(byteArrayOf(0, 4))
                    }

                    input.apply {
                        read() shouldBe 4
                        read() shouldBe 1022
                    }

                    val command = listOf(ADC_BASE, (ADC_CHANNEL_OFFSET + (signal - 1)).toByte())
                    mockRequests.shouldContainExactly(command + command)
                }
                test("Read (ATTINY 8X7)") {
                    // setup with the OTHER hardware
                    val input = testHatWithOtherBackpack.signal(signal).apply {
                        mode = SignalMode.INPUT
                        mockRequests.clear()
                    }

                    // data read
                    mockResponses.apply {
                        push(byteArrayOf(3, 0xFE.toByte()))
                        push(byteArrayOf(0, 4))
                    }

                    input.apply {
                        read() shouldBe 4
                        read() shouldBe 1022
                    }

                    val command = listOf(ADC_BASE, (ADC_CHANNEL_OFFSET + CRICKITHat.digitalPins[signal - 1]).toByte())
                    mockRequests shouldContainExactly command + command
                }

            }

            context("Signal $signal diozero:") {

            }
        }
    }
) {
    init {
        initProperties()
    }

    fun CRICKITSignal.setTestMode(m: SignalMode) {
        mode = m
        mockRequests.clear()
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
