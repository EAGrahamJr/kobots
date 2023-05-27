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
import crackers.kobots.utilities.elapsed
import java.time.Instant

class ControllerResponse(val armRequest: TheArm.Request, val proximityReading: Int)

/**
 * Manages set "states" of the system (one button trigger) - this will go away as things progress.
 */
object ControlThing : AutoCloseable {
    const val WAITING_TOO_LONG = 60
    const val IDLE_RESET = 30

    //    private var currentMode = Mode.IDLE
    private var lastDeployed: Instant? = null

    override fun close() {
    }

    fun execute(currentButtons: List<Boolean>): ControllerResponse {
        val scanButton = currentButtons[0]
        val calibrateButton = currentButtons[1]

        val armRequest = when (TheArm.state) {
            TheArm.State.REST -> {
                if (scanButton) {
                    Stripper.modeSelect(YELLOW)
                    TheArm.Request.DEPLOY_FRONT
                } else if (calibrateButton) {
                    TheArm.Request.CALIBRATE
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

        return ControllerResponse(armRequest, ProximitySensor.proximity)
    }

    /**
     * See if we trigger the retraction based on sensor or a delayed button.
     */
    private fun readyMode(trigger: Boolean): Boolean {
        val lastUsed = lastDeployed?.elapsed()?.toSeconds() ?: -1

        return if (ProximitySensor.tooClose) {
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
