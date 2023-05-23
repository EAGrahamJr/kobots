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

    private val proximitySensor by lazy {
        VCNL4040().apply {
            proximityEnabled = true
            ambientLightEnabled = true
        }
    }

    //    private var currentMode = Mode.IDLE
    private var lastDeployed: Instant? = null
    private val proximityReading = AtomicInteger(0)
    val proximity: Int
        get() = proximityReading.get()
    val tooClose: Boolean
        get() = proximity > CLOSE_ENOUGH

    override fun close() {
        proximitySensor.close()
    }

    fun execute(currentButtons: List<Boolean>): ControllerResponse {
        proximityReading.set(proximitySensor.proximity.toInt())

        val scanButton = currentButtons[0]
        val calibrateButton = currentButtons[1]

        val armRequest = when (TheArm.state) {
            TheArm.State.REST -> {
                if (scanButton) {
                    Stripper.modeSelect(YELLOW)
                    TheArm.Request.DEPLOY_FRONT
                } else {
                    Stripper.modeSelect(DEFAULT)
                    TheArm.Request.NONE
                }
            }

            TheArm.State.FRONT -> {
                if (lastDeployed == null) {
                    lastDeployed = Instant.now()
                    Stripper.modeSelect(LARSON)
                    TheArm.chomp()
                }
                if (readyMode(scanButton)) TheArm.Request.REST else TheArm.Request.NONE
            }

            else -> {
                lastDeployed = null
                TheArm.Request.NONE
            }
        }

        return ControllerResponse(armRequest, proximityReading.get())
    }

    /**
     * See if we trigger the retraction based on sensor or a delayed button.
     */
    private fun readyMode(trigger: Boolean): Boolean {
        val lastUsed = lastDeployed?.elapsed()?.toSeconds() ?: -1

        return proximity.let { prox ->
            if (prox > CLOSE_ENOUGH) {
                Stripper.modeSelect(RED)
                true
            } else if (lastUsed > IDLE_RESET && trigger) {
                Stripper.modeSelect(GREEN)
                true
            } else if (lastUsed > WAITING_TOO_LONG) {
                Stripper.modeSelect(BLUE)
                true
            } else false
        }
    }
}
