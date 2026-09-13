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

import crackers.kobots.mqtt.homeassistant.KobotSelectEntity
import crackers.kobots.parts.enumValue
import crackers.kobots.parts.movement.async.sceneBuilder
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import crackers.kobots.app.mechanicals.SuzerainOfServos as Suzi

private val logger = LoggerFactory.getLogger("Servomatic")

object Commando : KobotSelectEntity.Companion.SelectHandler {
    enum class Command {
        IDLE,
        SIMPLE,
        STOP,
    }

    override val options = Command.entries.map { it.name }.sorted()


    private fun suzi(act: suspend () -> Unit) = Suzi.executeAct(act)

    override fun executeOption(select: String) {
        val selected = enumValue<Command>(select)

        when (selected) {
            Command.IDLE -> {
                // TODO maybe show something stupid on the monitors?
            }

            Command.STOP -> {
                Suzi.stop()
                AppCommon.applicationRunning = false
            }

            Command.SIMPLE -> {
                suzi {
                    sceneBuilder {
                        defaultDuration = 2.seconds
                        Suzi.servos[6] smoothly {
                            startDelay = 1500.milliseconds
                            angle = 90
                            duration = 1.seconds
                        }
                        Suzi.servos[2] withSoftLanding {
                            angle = 90
                        }
                        Suzi.servos[5] withSoftLanding {
                            angle = 90
                        }
                        Suzi.servos[1] withSoftLanding {
                            startDelay = 50.milliseconds
                            angle = 90
                        }
                        Suzi.servos[4] withSoftLanding {
                            startDelay = 50.milliseconds
                            angle = 90
                        }
                        Suzi.servos[0] withSoftLanding {
                            startDelay = 100.milliseconds
                            angle = 90
                        }
                        Suzi.servos[3] withSoftLanding {
                            startDelay = 100.milliseconds
                            angle = 90
                        }
                    }()
                    delay(5.seconds)
                    Suzi.home()
                }
            }


            else -> logger.warn("No clue what to do with $select")
        }
    }
}
