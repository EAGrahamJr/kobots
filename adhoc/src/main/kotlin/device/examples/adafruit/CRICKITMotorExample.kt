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

package device.examples.adafruit

import crackers.kobots.devices.at
import device.examples.RunManager
import kobots.ops.createEventBus

/**
 * Run a motor using the analog inputs.
 */
class CRICKITMotorExample : RunManager() {
    init {
        createEventBus<Float>().also { bus ->
            val motor = crickit.motor(1)
            bus.registerConsumer { input ->
                // scale to motor
                motor at (input - .5f) * 2f
            }
            val xAxis = crickit.signalAnalogIn(6)
            bus.registerPublisher { xAxis.unscaledValue }
        }
    }

    fun run() {
        waitForIt()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty("diozero.remote.hostname", "marvin.local")

            CRICKITMotorExample().use {
                it.run()
            }
        }
    }
}
