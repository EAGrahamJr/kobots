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

import com.diozero.api.DeviceAlreadyOpenedException
import com.diozero.api.InvalidModeException
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.ADC_BASE
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.ADC_CHANNEL_OFFSET
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_BASE
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_BULK
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_BULK_CLEAR
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_BULK_SET
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_DIRECTION_INPUT
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_DIRECTION_OUTPUT
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_PULL_RESISTOR_DISABLED
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.GPIO_PULL_RESISTOR_ENABLED
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.SignalMode
import crackers.kobots.devices.expander.CRICKITHat.Companion.DIGITAL_PINS
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
        val factory = CRICKITHatDeviceFactory(testHat)

        (1..8).forEach { signal ->
            val pinSelectorBytes = signalPinSelectors[signal - 1]
            val blockSize = pinSelectorBytes.size

            context("Signal $signal setup") {
                test("Output") {
                    val setupCommand = digitalOutputSetupCommands(pinSelectorBytes)

                    factory.signal(signal).mode = SignalMode.OUTPUT
                    mockRequests shouldContainExactly setupCommand
                }
                test("Input") {
                    val setupCommand = analogInputSetupCommands(pinSelectorBytes)

                    factory.signal(signal).mode = SignalMode.INPUT
                    mockRequests shouldContainExactly setupCommand
                }
                test("Input with pull-up") {
                    factory.signal(signal).mode = SignalMode.INPUT_PULLUP
                    mockRequests shouldContainExactly inputPullUpSetupCommands(pinSelectorBytes)
                }
                test("Input with pull-down") {
                    val setupCommand = inputPullDownSetupCommands(pinSelectorBytes)

                    factory.signal(signal).mode = SignalMode.INPUT_PULLDOWN
                    mockRequests shouldContainExactly setupCommand
                }
            }

            context("Signal $signal digital output:") {
                // setup output
                val output = factory.signal(signal).also {
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
                val input = factory.signal(signal).apply {
                    mode = SignalMode.INPUT_PULLDOWN
                    mockRequests.clear()
                }

                test("Read") {
                    // set the response to match the mask, ergo "true"
                    mockResponses.push(ByteArray(blockSize) { index -> pinSelectorBytes[index] })

                    input.value shouldBe true
                    mockRequests shouldContainExactly readDigitalInputCommand
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
                    val input = factory.signal(signal).apply {
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
                    val input = CRICKITHatDeviceFactory(testHatWithOtherBackpack).signal(signal).apply {
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

                    val command = listOf(ADC_BASE, (ADC_CHANNEL_OFFSET + DIGITAL_PINS[signal - 1]).toByte())
                    mockRequests shouldContainExactly command + command
                }
            }

            context("Signal $signal diozero:") {
                val deviceFactory = CRICKITHatDeviceFactory(testHat)
                test("DigitalInput") {
                    deviceFactory.signalDigitalIn(signal, true).use {
                        mockRequests shouldContainExactly inputPullDownSetupCommands(pinSelectorBytes)
                        mockRequests.clear()

                        // set the response to match the mask, ergo "true"
                        mockResponses.push(ByteArray(blockSize) { index -> pinSelectorBytes[index] })
                        it.value shouldBe true
                        mockRequests shouldContainExactly readDigitalInputCommand
                    }
                }
                test("DigitalOutput") {
                    deviceFactory.signalDigitalOut(signal).use {
                        mockRequests shouldContainExactly digitalOutputSetupCommands(pinSelectorBytes)
                        mockRequests.clear()

                        val turnOnCommand = listOf(GPIO_BASE, GPIO_BULK_SET) + pinSelectorBytes
                        it.on()
                        mockRequests shouldContainExactly turnOnCommand
                    }
                }
                test("AnalogInput") {
                    deviceFactory.signalAnalogIn(signal).use {
                        mockRequests shouldContainExactly analogInputSetupCommands(pinSelectorBytes)
                        mockRequests.clear()

                        // data read
                        mockResponses.apply {
                            push(byteArrayOf(3, 0xFE.toByte()))
                            push(byteArrayOf(0, 4))
                        }

                        it.apply {
                            scaledValue.toInt() shouldBe 4
                            scaledValue.toInt() shouldBe 1022
                        }

                        val command = listOf(ADC_BASE, (ADC_CHANNEL_OFFSET + (signal - 1)).toByte())
                        mockRequests.shouldContainExactly(command + command)
                    }
                }
            }
        }
        context("Device conflicts") {
            test("Between digital out and analog in on the same port") {
                factory.signalAnalogIn(1).use {
                    shouldThrowWithMessage<DeviceAlreadyOpenedException>("Device 'CRICKIT-SIGNAL-101' is already opened") {
                        factory.signalDigitalOut(1)
                    }
                }
            }
        }
    }
) {
    init {
        initProperties()
    }
}

private fun analogInputSetupCommands(pinSelectorBytes: List<Byte>): List<Byte> {
    val directionOutputSetup = listOf(GPIO_BASE, GPIO_DIRECTION_INPUT)
    val disablePullResistor = listOf(GPIO_BASE, GPIO_PULL_RESISTOR_DISABLED)
    val setupCommand =
        directionOutputSetup + pinSelectorBytes +
            disablePullResistor + pinSelectorBytes
    return setupCommand
}

private fun digitalOutputSetupCommands(pinSelectorBytes: List<Byte>): List<Byte> {
    val directionOutputSetup = listOf(GPIO_BASE, GPIO_DIRECTION_OUTPUT)
    val setupCommand = directionOutputSetup + pinSelectorBytes
    return setupCommand
}

private fun inputPullDownSetupCommands(pinSelectorBytes: List<Byte>): List<Byte> {
    val directionOutputSetup = listOf(GPIO_BASE, GPIO_DIRECTION_INPUT)
    val enablePullResistor = listOf(GPIO_BASE, GPIO_PULL_RESISTOR_ENABLED)
    val bulkClear = listOf(GPIO_BASE, GPIO_BULK_CLEAR)

    val setupCommand =
        directionOutputSetup + pinSelectorBytes +
            enablePullResistor + pinSelectorBytes +
            bulkClear + pinSelectorBytes
    return setupCommand
}

private fun inputPullUpSetupCommands(pinSelectorBytes: List<Byte>): List<Byte> {
    val directionOutputSetup = listOf(GPIO_BASE, GPIO_DIRECTION_INPUT)
    val enablePullResistor = listOf(GPIO_BASE, GPIO_PULL_RESISTOR_ENABLED)
    val bulkSet = listOf(GPIO_BASE, GPIO_BULK_SET)

    val setupCommand =
        directionOutputSetup + pinSelectorBytes +
            enablePullResistor + pinSelectorBytes +
            bulkSet + pinSelectorBytes
    return setupCommand
}

private val readDigitalInputCommand = listOf(GPIO_BASE, GPIO_BULK)

private val signalPinSelectors = arrayOf<List<Byte>>(
    listOf(0x00, 0x00, 0x00, 0x04),
    listOf(0x00, 0x00, 0x00, 0x08),
    listOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00),
    listOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00),
    listOf(0x00, 0x00, 0x08, 0x00),
    listOf(0x00, 0x00, 0x04, 0x00),
    listOf(0x00, 0x00, 0x02, 0x00),
    listOf(0x00, 0x00, 0x01, 0x00)
)
