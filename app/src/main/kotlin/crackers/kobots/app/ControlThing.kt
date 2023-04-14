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

import crackers.kobots.app.StatusFlags.armStatus
import crackers.kobots.app.TheArm.ArmState.*
import crackers.kobots.devices.sensors.VCNL4040
import crackers.kobots.utilities.elapsed
import java.time.Instant

/**
 * servo and "control" - currently a proxmity sensor
 */
object ControlThing : AutoCloseable {
    const val CLOSE_ENOUGH = 15
    const val ACTIVATION_PADDING = 15
    const val IDLE_RESET = 30

    private enum class Mode {
        IDLE, EXTEND, READY, RETRACT
    }

    val proximitySensor by lazy {
        VCNL4040().apply {
            proximityEnabled = true
            ambientLightEnabled = true
        }
    }

    private var currentMode = Mode.IDLE
    private var lastDeployed = Instant.EPOCH

    override fun close() {
        proximitySensor.close()
    }

    fun activateSensor() {
        when (currentMode) {
            Mode.IDLE -> currentMode = Mode.EXTEND
            Mode.READY -> if (lastDeployed.elapsed().toSeconds() > ACTIVATION_PADDING) currentMode = Mode.RETRACT
            else -> Unit
        }
    }

    fun execute() {
        val armStatus = armStatus.get()

        when (currentMode) {
            Mode.EXTEND -> {
                when (armStatus) {
                    AT_FRONT -> {
                        currentMode = Mode.READY
                        lastDeployed = Instant.now()
                    }

                    AT_REST -> StatusFlags.armStatus.set(DEPLOY_FRONT)
                    else -> Unit
                }
            }

            Mode.RETRACT -> {
                when (armStatus) {
                    AT_FRONT -> StatusFlags.armStatus.set(REST)
                    AT_REST -> currentMode = Mode.IDLE
                    else -> Unit
                }
            }

            Mode.READY -> {
                proximitySensor.proximity.toInt().let { prox ->
                    StatusFlags.proximityReading.set(prox)
                    // specific trigger
                    // TODO should be state variable?
                    if (prox > CLOSE_ENOUGH) {
                        Stripper.modeChange()
                        currentMode = Mode.RETRACT
                    } else if (lastDeployed.elapsed().toSeconds() > IDLE_RESET) {
                        currentMode = Mode.RETRACT
                    }
                }
            }

            else -> Unit
        }
    }
}
