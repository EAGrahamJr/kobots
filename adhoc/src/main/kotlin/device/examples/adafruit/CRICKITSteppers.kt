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
import com.diozero.devices.sandpit.motor.BasicStepperController.StepStyle
import com.diozero.devices.sandpit.motor.BasicStepperMotor
import com.diozero.devices.sandpit.motor.StepperMotorInterface
import com.diozero.util.SleepUtil
import crackers.kobots.devices.expander.CRICKITHatDeviceFactory
import java.time.Duration

/**
 * Both blocks. Note that direction may not be as expected due to wiring change.
 */
class CRICKITSteppers {
    val crickit by lazy { CRICKITHatDeviceFactory() }

    val uniStep by lazy { BasicStepperMotor(512, crickit.unipolarStepperPort()) }
    val biStep by lazy { BasicStepperMotor(200, crickit.motorStepperPort()) }
    val stopTouch by lazy {
        crickit.touchDigitalIn(4).apply {
            value
        }
    }

    fun execute() {
        crickit.use {
            uniStep.use {
                it.doStuff()
                it.release()
            }
            SleepUtil.sleepSeconds(3)
            biStep.use {
                it.doStuff()
                it.release()
            }
        }
    }

    fun BasicStepperMotor.doStuff(style: StepStyle = StepStyle.SINGLE) {
        (1..getStepsForStyle(style)).forEach {
            step(StepperMotorInterface.Direction.FORWARD, style)
            SleepUtil.busySleep(Duration.ofMillis(3).toNanos())
            if (stopTouch.value) return
        }
        (1..getStepsForStyle(style)).forEach {
            step(StepperMotorInterface.Direction.BACKWARD, style)
            SleepUtil.busySleep(Duration.ofMillis(3).toNanos())
            if (stopTouch.value) return
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty(REMOTE_PI, "marvin.local")

            CRICKITSteppers().execute()
        }
    }
}
