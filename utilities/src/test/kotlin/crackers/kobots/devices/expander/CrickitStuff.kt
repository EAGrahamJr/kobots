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

import crackers.kobots.devices.MockI2CDevice
import io.kotest.core.spec.style.FunSpec
import java.util.*

/**
 * Stuff for testing
 */
fun initProperties() {
    System.setProperty(
        com.diozero.sbc.DeviceFactoryHelper.DEVICE_FACTORY_PROP,
        "com.diozero.internal.provider.mock.MockDeviceFactory"
    )
}

fun goodInitResponses(): Stack<ByteArray>.() -> Unit = {
    push(byteArrayOf(0x0f, 0x75, 0x27, 0x01))
    push(byteArrayOf(AdafruitSeeSaw.Companion.DeviceType.SAMD09_HW_ID_CODE.pid.toByte()))
}

val testHat: CrickitHat by lazy {
    MockI2CDevice.responses.apply(goodInitResponses())
    CrickitHat(MockI2CDevice.device).also {
        MockI2CDevice.requests.clear()
    }
}

fun FunSpec.clearBeforeTest() {
    beforeTest {
        MockI2CDevice.requests.clear()
        MockI2CDevice.responses.clear()
    }
}
