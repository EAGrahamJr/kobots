/*
 * Copyright 2022-2026 by E. A. Graham, Jr.
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

import com.diozero.devices.sandpit.motor.BasicStepperController
import com.diozero.devices.sandpit.motor.BasicStepperMotor
import com.diozero.devices.sandpit.motor.StepperMotorInterface.Direction
import crackers.kobots.parts.movement.async.AsyncStepperRotator
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * TODO fill this in
 */
open class CalibratingRotator(
    val myStepper: BasicStepperMotor,
    private val stopFunction: () -> Boolean,
    val gearRatio: Float = 1f,
    val reversed: Boolean = false,
    stepStyle: BasicStepperController.StepStyle = BasicStepperController.StepStyle.SINGLE,
    stepPause: Duration = Duration.ZERO,
) : AsyncStepperRotator(
    myStepper, gearRatio, reversed, stepStyle, stepPause
) {
    private var calibrating = AtomicBoolean(false)
    private val logger = LoggerFactory.getLogger("CalibratingRotator")

    fun runCalibration() = runBlocking {
        if (!calibrating.compareAndSet(false, true)) return@runBlocking

        logger.warn("Calibration starting")
        runCatching {
            // run the stepper out 1/4 of it's arc, modified by gear ratio
            val out = (myStepper.stepsPerRotation * gearRatio / 4).toInt()
            val (outDirection, inDirection) = if (reversed)
                Pair(Direction.BACKWARD, Direction.FORWARD)
            else
                Pair(Direction.FORWARD, Direction.BACKWARD)
            repeat(out) {
                myStepper.step(outDirection)
                delay(2.milliseconds)
            }
            // bring it back checking the stop function
            for (ignored in 0 until out * 2) {
                if (stopFunction()) break
                myStepper.step(inDirection)
                delay(2.milliseconds)
            }
        }.map {
            logger.warn("Calibration completed")
            calibrating.set(false)
        }
    }

}
