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

import base.REMOTE_PI
import com.diozero.devices.sandpit.motor.BYJ48Stepper
import crackers.kobots.ops.createEventBus
import device.examples.RunManagerForFlows

/**
 * TODO fill this in
 */
class CRICKITSteppers : RunManagerForFlows() {
    val stepper = BYJ48Stepper(crickit.unipolarStepper(true))

    init {
        createEventBus<Boolean>(name = "Plus").apply {
            val button = crickit.touchDigitalIn(1)
            registerPublisher { button.value }
            registerConditionalConsumer(errorMessageHandler, { it }) {
                logger.info("Plus")
                stepper.rotate(15f)
            }
        }
        createEventBus<Boolean>(name = "Minus").apply {
            val button = crickit.touchDigitalIn(2)
            registerPublisher { button.value }
            registerConditionalConsumer(errorMessageHandler, { it }) {
                logger.info("Minus")
                stepper.rotate(-15f)
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty(REMOTE_PI, "marvin.local")

            CRICKITSteppers().use {
                it.waitForIt()
            }
        }
    }
}
