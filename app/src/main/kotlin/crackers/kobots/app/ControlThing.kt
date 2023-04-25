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
import crackers.kobots.devices.lighting.NeoKey
import crackers.kobots.devices.lighting.PixelBuffer.PixelColor
import crackers.kobots.devices.sensors.VCNL4040
import crackers.kobots.utilities.GOLDENROD
import crackers.kobots.utilities.elapsed
import java.awt.Color
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

    val keyboard by lazy {
        NeoKey().apply { pixels.brightness = 0.05f }
    }

    private var currentMode = Mode.IDLE
    private var lastDeployed = Instant.EPOCH

    override fun close() {
        proximitySensor.close()
        keyboard.close()
    }

    fun execute() {
        val armStatus = armStatus.get().also {
            if (!it.name.startsWith("AT")) keyboard[0] = GOLDENROD
        }

        if (keyboard[3]) {
            keyboard[3] = PixelColor(Color.RED, brightness = 0.05f)
            // TODO something
            keyboard[3] = PixelColor(Color.GREEN, brightness = 0.01f)
        }

        when (currentMode) {
            Mode.EXTEND -> {
                when (armStatus) {
                    AT_FRONT -> {
                        currentMode = Mode.READY
                        lastDeployed = Instant.now()
                    }

                    AT_REST -> {
                        StatusFlags.armStatus.set(DEPLOY_FRONT)
                        keyboard[0] = Color.RED
                    }
                    else -> Unit
                }
            }

            Mode.RETRACT -> {
                when (armStatus) {
                    AT_FRONT -> StatusFlags.armStatus.set(REST)
                    AT_REST -> {
                        currentMode = Mode.IDLE
                        keyboard[0] = Color.BLACK
                    }
                    else -> Unit
                }
            }

            Mode.READY -> {
                keyboard[0] = Color.GREEN
                proximitySensor.luminosity.toInt().let { prox ->
                    StatusFlags.proximityReading.set(prox)
                    // specific trigger
                    // TODO should be state variable?
                    if (prox > CLOSE_ENOUGH) {
                        Stripper.modeChange()
                        currentMode = Mode.RETRACT
                    } else if (lastDeployed.elapsed().toSeconds() > IDLE_RESET || keyboard[0]) {
                        currentMode = Mode.RETRACT
                    }
                }
            }

            else -> if (keyboard[0]) currentMode = Mode.EXTEND
        }
    }
}
