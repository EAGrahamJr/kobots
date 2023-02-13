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

import crackers.kobots.devices.at
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.TIMER_BASE
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.TIMER_PWM
import crackers.kobots.devices.expander.CRICKITHat.Companion.MOTORS
import crackers.kobots.devices.expander.CRICKITHat.Companion.PWM_PINS
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import crackers.kobots.devices.MockI2CDevice.requests as mockRequests

class CRICKITMotorDeviceTest : FunSpec(
    {
        clearBeforeTest()

        val factory = CRICKITHatDeviceFactory(testHat)

        (1..2).forEach { motorId ->
            val deviceId = if (motorId == 1) 0 else 2
            val posPin = listOf(TIMER_BASE, TIMER_PWM, PWM_PINS.indexOf(MOTORS[deviceId]).toByte())
            val negPin = listOf(TIMER_BASE, TIMER_PWM, PWM_PINS.indexOf(MOTORS[deviceId + 1]).toByte())
            val fullOn = listOf(0xFF.toByte(), 0xFF.toByte())
            val fullOff = listOf(0x00.toByte(), 0x00)
            val halfOn = listOf(0x7F.toByte(), 0xFF.toByte())
            val fullStop = fullOn // interesting! - hard stop supplies current to both directions

            val motor = factory.motor(motorId)

            listOf(
                1f to negPin + fullOff + posPin + fullOn,
                .5f to negPin + fullOff + posPin + halfOn,
                0f to negPin + fullOff + posPin + fullOff,
                -.5f to posPin + fullOff + negPin + halfOn,
                -1f to posPin + fullOff + negPin + fullOn
            )
                .forEach { p ->
                    test("Run motor $motorId to ${p.first}") {
                        motor at p.first
                        mockRequests.shouldContainExactly(p.second)
                    }
                }
        }


    }) {

    init {
        initProperties()
    }
}
