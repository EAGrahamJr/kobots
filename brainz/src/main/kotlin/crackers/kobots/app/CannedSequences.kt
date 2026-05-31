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

import crackers.kobots.app.Jimmy.driveStepperRotator
import crackers.kobots.app.Jimmy.motorStepperRotator
import crackers.kobots.app.Jimmy.rotor3
import crackers.kobots.app.Jimmy.rotorElevation
import crackers.kobots.app.Jimmy.rotorH
import crackers.kobots.app.Jimmy.rotorV
import crackers.kobots.parts.movement.async.AsyncRotator
import crackers.kobots.parts.movement.async.EventBus
import crackers.kobots.parts.movement.async.KobotsEvent
import crackers.kobots.parts.movement.async.sceneBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.lang.Thread.sleep
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

const val RUN_STUFF = "RUN_STUFF"

/**
 * Ibid
 */
object CannedSequences {

    class MoveMessage(
        val runThis: suspend () -> Unit,
    ) : KobotsEvent {
        override val name = RUN_STUFF
        fun publish() {
            val msg = this
            runBlocking {
                EventBus.publish(msg)
            }
        }
    }

    private val home = sceneBuilder {
        rotorV withSoftLanding {
            angle = 0
            duration = 3.seconds
        }
        rotorH withSoftLanding {
            angle = 0
            duration = 1.seconds
        }
        rotor3 withSoftLanding {
            angle = 0
            duration = 2.seconds
        }
        rotorElevation withSoftLanding {
            angle = 0
            duration = 2.seconds
        }
        motorStepperRotator withSoftLanding {
            angle = 0
            duration = 5.seconds
        }
        driveStepperRotator withSoftLanding {
            angle = 0
            duration = 5.seconds
        }
    }

    fun goHome() {
        runBlocking {
            home()
        }
    }

    fun wave_420() {
        val upScene = sceneBuilder {
            rotorV smoothly {
                angle = 180
                duration = 4.seconds
            }
            rotorH withSoftLanding {
                startDelay = 500.milliseconds
                angle = 180
                duration = 2.5.seconds
            }
        }
        val downScene = sceneBuilder {
            rotorV smoothly {
                angle = 0
                duration = 4.seconds
            }
            rotorH withSoftLanding {
                angle = 0
                duration = 2.seconds
            }
        }
        MoveMessage {
            upScene()
            sleep(500.milliseconds.inWholeMilliseconds)
            downScene()
        }.publish()
    }

    fun frontThing() {
        val testIt = sceneBuilder {
            motorStepperRotator smoothly {
                angle = 350
                duration = 5.seconds
            }
        }
        val parkIt = sceneBuilder {
            motorStepperRotator withSoftLanding {
                angle = 0
                duration = 8.seconds
            }
        }
        MoveMessage {
            val elapsedMs = kotlin.system.measureTimeMillis { testIt() }
            println("play took ${elapsedMs} ms")
            sleep(500.milliseconds.inWholeMilliseconds)
            parkIt()
        }.publish()
    }

    fun everybodyAllAtOnce() {
        val fullScene = sceneBuilder {
            rotorV withSoftLaunch {
                angle = 90
                duration = 6.seconds
            }
            rotorH smoothly {
                startDelay = 1.seconds
                angle = 90
                duration = 3.seconds
            }
            rotor3 withSoftLanding {
                angle = 180
                duration = 4.seconds
            }
            motorStepperRotator withSoftLanding {
                angle = 180
                duration = 10.seconds
            }
        }
        MoveMessage {
            fullScene()
            delay(2.seconds)
            home()
            motorStepperRotator.release()
        }.publish()
    }

    fun rotatorGo(whichRotator: AsyncRotator, whereTo: Int) {
        MoveMessage {
            sceneBuilder {
                whichRotator smoothly {
                    angle = whereTo
                    duration = if (abs(whereTo - whichRotator.current) > 10) 2.seconds else .5.seconds
                }
            }.invoke()
        }.publish()
    }
}
