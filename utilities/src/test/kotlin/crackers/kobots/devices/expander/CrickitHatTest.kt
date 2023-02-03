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

import com.diozero.api.RuntimeIOException
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.STATUS_BASE
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.STATUS_HW_ID
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.STATUS_SWRST
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.STATUS_VERSION
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.assertThrows
import crackers.kobots.devices.MockI2CDevice.device as mockDevice
import crackers.kobots.devices.MockI2CDevice.requests as mockRequests
import crackers.kobots.devices.MockI2CDevice.responses as mockResponses

/**
 * Test data values were captured from the Adafruit Circuit Python code directly
 */
class CrickitHatTest : FunSpec(
    {
        clearBeforeTest()

        context("Seesaw startup") {
            test("Seesaw initialization") {
                mockResponses.apply(goodInitResponses())

                // initializes
                CrickitHat(mockDevice)

                // contains reset command, get board, get version
                val expectedCommandBytes = listOf(STATUS_BASE, STATUS_SWRST, 0xff.toByte()) +
                        listOf(STATUS_BASE, STATUS_HW_ID) +
                        listOf(STATUS_BASE, STATUS_VERSION)

                mockRequests.shouldContainExactly(expectedCommandBytes)
            }

            test("Seesaw initialization with bad hardware") {
                mockResponses.push(byteArrayOf(0xFF.toByte()))
                assertThrows<RuntimeIOException> {
                    CrickitHat(mockDevice)
                }
            }
        }
    }
) {
    init {
        initProperties()
    }
}
