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

package crackers.kobots.app

import crackers.kobots.app.Stripper.StripColors.*
import crackers.kobots.devices.sensors.VCNL4040
import crackers.kobots.utilities.elapsed
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class ControllerResponse(val armRequest: TheArm.Request, val proximityReading: Int)

/**
 * Manages set "states" of the system (one button trigger) - this will go away as things progress.
 */
object ControlThing : AutoCloseable {
    const val CLOSE_ENOUGH = 15
    const val WAITING_TOO_LONG = 60
    const val IDLE_RESET = 30

    private enum class Mode {
        IDLE, EXTEND, READY, RETRACT
    }

    private val proximitySensor by lazy {
        VCNL4040().apply {
            proximityEnabled = true
            ambientLightEnabled = true
        }
    }

    private var currentMode = Mode.IDLE
    private var lastDeployed = Instant.EPOCH
    private var proximityReading = AtomicInteger(0)
    val proximity: Int
        get() = proximityReading.get()
    val tooClose: Boolean
        get() = proximity > CLOSE_ENOUGH

    override fun close() {
        proximitySensor.close()
    }

    fun execute(currentButtons: List<Boolean>): ControllerResponse {
        val myButton = currentButtons[0]

        val armStatus = TheArm.state
        val armRequest = when (currentMode) {
            Mode.EXTEND -> {
                // arrived
                if (armStatus == TheArm.State.FRONT) {
                    currentMode = Mode.READY
                    lastDeployed = Instant.now()
                    Stripper.modeSelect(LARSON)
                }
                TheArm.Request.NONE
            }

            Mode.RETRACT -> {
                if (armStatus == TheArm.State.REST) {
                    currentMode = Mode.IDLE
                    Stripper.modeSelect(DEFAULT)
                    proximityReading.set(0)
                }
                TheArm.Request.NONE
            }

            Mode.READY -> readyMode(myButton)
            else -> {
                // run it out
                if (myButton) {
                    currentMode = Mode.EXTEND
                    Stripper.modeSelect(YELLOW)
                    TheArm.Request.DEPLOY_FRONT
                } else {
                    TheArm.Request.NONE
                }
            }
        }

        return ControllerResponse(armRequest, proximityReading.get())
    }

    /**
     * See if we trigger the retraction based on sensor or a delayed button.
     */
    private fun readyMode(trigger: Boolean): TheArm.Request {
        val lastUsed = lastDeployed.elapsed().toSeconds()

        proximitySensor.proximity.toInt().let { prox ->
            proximityReading.set(prox)
            // specific trigger
            // TODO should be state variable?
            if (prox > CLOSE_ENOUGH) {
                Stripper.modeSelect(RED)
                currentMode = Mode.RETRACT
            } else if (lastUsed > IDLE_RESET && trigger) {
                currentMode = Mode.RETRACT
                Stripper.modeSelect(GREEN)
            } else if (lastUsed > WAITING_TOO_LONG) {
                currentMode = Mode.RETRACT
                Stripper.modeSelect(BLUE)
            }
        }
        return if (currentMode == Mode.RETRACT) TheArm.Request.REST else TheArm.Request.NONE
    }
}
