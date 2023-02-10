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

import com.diozero.api.AnalogInputDevice
import com.diozero.api.DigitalInputDevice
import com.diozero.api.GpioEventTrigger
import com.diozero.api.GpioPullUpDown
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.TOUCH_BASE
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.TOUCH_CHANNEL_OFFSET
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import crackers.kobots.devices.MockI2CDevice.requests as mockRequests
import crackers.kobots.devices.MockI2CDevice.responses as mockResponses

/**
 * Test data values were captured from the Adafruit Circuit Python code directly
 */
class CrickitHatTouchPadsTest : FunSpec(
    {
        clearBeforeTest()

        // present the same data for each test
        beforeTest {
            mockResponses.apply {
                push(byteArrayOf(0x3, 0xFF.toByte()))   // full value
                push(byteArrayOf(0x02, 0x00))           // "on" value
                push(byteArrayOf(0x00, 0x2C))           // "off" value
            }
        }

        (1..4).forEach { touchPad ->
            context("TouchPad $touchPad:") {
                test("Inputs") {
                    // because the first read is for a binary value, capture the 5 calibration reads
                    (1..5).forEach {
                        mockResponses.push(byteArrayOf(0x01, 0x2C))
                    }
                }
                (testHat.touch(touchPad)).apply {
                    isOn shouldBe false
                    isOn shouldBe true
                    value shouldBe 1023
                }

                // the above should generate 8 requests
                val expectedRegistryCalls = mutableListOf<Byte>()
                repeat(8) {
                    expectedRegistryCalls += TOUCH_BASE
                    expectedRegistryCalls += (TOUCH_CHANNEL_OFFSET + touchPad - 1).toByte()
                }
                mockRequests shouldContainExactly expectedRegistryCalls
            }


            context("TouchPad $touchPad diozero:") {
                val factory = CRICKITHatDeviceFactory(testHat)
                test("Create analog device") {
                    // N.B. this should not trigger the calibration reads
                    AnalogInputDevice(factory, CRICKITHatDeviceFactory.Types.TOUCH.deviceNumber(touchPad)).use { d ->
                        d.unscaledValue shouldBe 0.043010753f
                        d.unscaledValue shouldBe 0.50048876f
                        d.scaledValue shouldBe 1023f
                    }
                    // the above should generate 3 requests
                    val expectedRegistryCalls = mutableListOf<Byte>()
                    repeat(3) {
                        expectedRegistryCalls += TOUCH_BASE
                        expectedRegistryCalls += (TOUCH_CHANNEL_OFFSET + touchPad - 1).toByte()
                    }
                    mockRequests shouldContainExactly expectedRegistryCalls
                }

                test("Create digital device") {
                    // because each read is for a binary value, capture the 5 calibration reads
                    (1..5).forEach {
                        mockResponses.push(byteArrayOf(0x01, 0x2C))
                    }

                    DigitalInputDevice(
                        factory, CRICKITHatDeviceFactory.Types.TOUCH.deviceNumber(touchPad),
                        GpioPullUpDown.NONE, GpioEventTrigger.NONE
                    ).use { d ->
                        d.value shouldBe false
                        d.value shouldBe true
                        d.value shouldBe true
                    }

                    // the above should generate 8 requests
                    val expectedRegistryCalls = mutableListOf<Byte>()
                    repeat(8) {
                        expectedRegistryCalls += TOUCH_BASE
                        expectedRegistryCalls += (TOUCH_CHANNEL_OFFSET + touchPad - 1).toByte()
                    }
                    mockRequests shouldContainExactly expectedRegistryCalls
                }
            }
        }
    }
) {

    init {
        initProperties()
    }
}
