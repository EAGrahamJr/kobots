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

package dork

import base.minutes
import com.diozero.devices.sandpit.motor.BYJ48Stepper
import com.diozero.devices.sandpit.motor.BasicStepperController.GpioFiveWireUnipolarController
import com.diozero.devices.sandpit.motor.StepperMotorInterface
import java.lang.Thread.sleep

fun main() {
    val controller = GpioFiveWireUnipolarController(intArrayOf(18, 23, 24, 25), true)
    BYJ48Stepper(controller).apply {
        println("RPM = $defaultSpeed, freq = $defaultFrequency")
        first()
//        dalek()
        println("Singles")
        sleep(2000)
        (1..512).forEach {
            step(StepperMotorInterface.Direction.CLOCKWISE)
        }
    }.close()
}

private fun BYJ48Stepper.first() {
    stop()
    sleep(2000)
    1 minutes {
        rotate(360f)
        sleep(2000)
        rotate(-360f)
        sleep(2000)
    }
    onMove { println("moving slowly") }
    onStop { println("stopped") }
    start(StepperMotorInterface.Direction.CLOCKWISE, 10f)
    sleep(15_000)
    stop()
    println("done")
}

fun BYJ48Stepper.dalek() {
    stop()
    1 minutes {
        rotate(15f)
        rotate(-15f)
    }
}
