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

import io.kotest.core.spec.style.FunSpec

class CRICKITMotorDeviceTest : FunSpec(
    {
        (1..2).forEach { motorId ->
//            val deviceId = MOTORS[motorId - 1]
//            val posPin = PWM_PINS.indexOf(deviceId.positive.toInt())
//            val negPin = PWM_PINS.indexOf(deviceId.negative.toInt())
//            val commandPrefix = listOf(TIMER_BASE, TIMER_PWM)
//            val fullOn = listOf(0xFF.toByte(), 0xFF.toByte())
//            val fullOff = listOf<Byte>(0x00, 0x00)
//            val halfOn = listOf(0x7F.toByte(), 0xFF.toByte())
//            val fullStop = fullOn // interesting! - hard stop supplies current to both directions

            test("Run motor $motorId") {
//                testHat.motor(motorId).let { motor ->
//                    motor at 1f
//                    motor at .5f
//                    motor at 0f
//                    motor at -.5f
//                    motor at -1f
//                    motor at 0f
//                }
            }
        }

    })
/*
Dual motor demo!
terminals (22, 23)
1x forward -----------------------
hi 255 lo 255
POS Debug - write 080106ffff
hi 0 lo 0
NEG Debug - write 0801070000
1/2 forward -----------------------
hi 127 lo 255
Debug - write 0801067fff
hi 0 lo 0
Debug - write 0801070000
stop -----------------------
hi 255 lo 255
Debug - write 080106ffff
hi 255 lo 255
Debug - write 080107ffff
1/2 back -----------------------
hi 0 lo 0
Debug - write 0801060000
hi 127 lo 255
Debug - write 0801077fff
1x back -----------------------
hi 0 lo 0
Debug - write 0801060000
hi 255 lo 255
Debug - write 080107ffff
stop -----------------------
hi 255 lo 255
Debug - write 080106ffff
hi 255 lo 255
Debug - write 080107ffff
 */
