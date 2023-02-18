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

package crackers.kobots.devices.lighting

import bytes
import crackers.kobots.devices.expander.clearBeforeTest
import crackers.kobots.utilities.hex
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import org.slf4j.LoggerFactory
import crackers.kobots.devices.MockI2CDevice.device as mockDevice
import crackers.kobots.devices.MockI2CDevice.requests as mockRequests

class IS31FL3731Test : FunSpec(
    {
        val testLogger = LoggerFactory.getLogger("IS31 Test")

        clearBeforeTest()

        test("Setup") {
            val regConfig = mutableListOf(0x00).apply {
                (1..18).forEach { add(0xFF) }
                add(0x24)
                (1..24).forEach { add(0x00) }
                add(0x3c)
                (1..24).forEach { add(0x00) }
                add(0x54)
                (1..24).forEach { add(0x00) }
                add(0x6c)
                (1..24).forEach { add(0x00) }
                add(0x84)
                (1..24).forEach { add(0x00) }
                add(0x9c)
                (1..24).forEach { add(0x00) }
            }.bytes()

            val is31 = object : IS31FL3731(mockDevice) {
                override val width = 1
                override val height = 1
            }

            // slice up the requests and check each piece: this first one selects the config bank and turns off the
            // display
            mockRequests.subList(0, 4).apply {
                this shouldContainExactly listOf(_BANK_ADDRESS, _CONFIG_BANK, _SHUTDOWN_REGISTER, 0).bytes()
                clear()
            }

            mockRequests.subList(0, 14).apply {
                this shouldContainExactly mutableListOf<Int>().apply {
                    (1..14).forEach { add(0) }
                }.bytes()
                clear()
            }
            (0..7).forEach { frame ->
                mockRequests.subList(0, 2).apply {
                    testLogger.debug("block1 ${this.map { it.hex() }.joinToString("")}")
                    this shouldContainExactly listOf(_BANK_ADDRESS, frame).bytes()
                    clear()
                }
                mockRequests.subList(0, regConfig.size).apply {
                    testLogger.debug("block1 ${this.map { it.hex() }.joinToString("")}")
                    testLogger.debug("block2 ${regConfig.map { it.hex() }.joinToString("")}")
                    this shouldContainExactly regConfig
                    clear()
                }

            }
            // the remainder should be a config select and turn the display on
            mockRequests shouldContainExactly listOf(_BANK_ADDRESS, _CONFIG_BANK, _SHUTDOWN_REGISTER, 1)
                .bytes()
        }

        context("Write color to LED Shim") {
            val shim = PimoroniLEDShim(mockDevice)
            /**
             * This device consists of 3 rows of LEDs - RGB
             */
            listOf("red", "green", "blue").forEachIndexed { y, color ->
                for (i in 1..10)
                    test("write $color to offset $i, increasing brightness to ${i}0%") {
                        val pixelColor = (i * 10) / 255
                        shim.pixel(i - 1, y, pixelColor)
                        val addr = shim.pixelAddress(i - 1, y)
                        mockRequests shouldContainExactly
                            if (i == 1 && y == 0) listOf(_BANK_ADDRESS, 0x00, _COLOR_OFFSET + addr, pixelColor).bytes()
                            else listOf(_COLOR_OFFSET + addr, pixelColor).bytes()
                    }
            }
        }
    }
)
