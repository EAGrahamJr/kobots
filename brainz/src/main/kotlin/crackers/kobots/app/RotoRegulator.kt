/*
 * Copyright 2022-2024 by E. A. Graham, Jr.
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

package crackers.kobots.app

/**
 * Rotary encoder stuff. This should make stuff move depending on mode.
 */
object RotoRegulator {
//    val encoder by lazy { QwiicTwist(i2cMultiplexer.getI2CDevice(2, QwiicTwist.DEFAULT_I2C_ADDRESS)) }
//
//    init {
//        encoder.count = 0 // always start at zero
//        encoder.pixel.brightness = 0.25f
//        encoder.pixel + PURPLE
//        encoder.colorConnection = Triple(0, 0, 0)
//    }
//
//    private var buttonDown = false
//    private var lastCount = 0
//
//    fun readTwist() {
//        // pressing the button allows switching to and from "manual" mode
//        buttonDown =
//            (encoder button PRESSED).also { pressed ->
//                if (pressed && !buttonDown) {
//                    //            killAllTheThings()
//                    currentMode = if (currentMode == Mode.MANUAL) Mode.NONE else Mode.MANUAL
//                }
//            }
//
//        lastCount =
//            encoder.count.let {
//                if (currentMode == Mode.MANUAL && it > lastCount) {
//                    BackBenchPicker.updateMenu()
//                    FrontBenchPicker.updateMenu()
//                }
//                it
//            }
//    }
//
}
