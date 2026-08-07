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
import crackers.kobots.app.Jimmy.neoPixel
import crackers.kobots.app.Jimmy.rotor1
import crackers.kobots.app.Jimmy.rotor2
import crackers.kobots.app.Jimmy.rotor3
import crackers.kobots.app.Jimmy.rotor4
import crackers.kobots.devices.lighting.WS2811
import crackers.kobots.parts.movement.async.AsyncRotator
import crackers.kobots.parts.movement.async.EventBus
import crackers.kobots.parts.movement.async.KobotsEvent
import crackers.kobots.parts.movement.async.sceneBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.awt.Color
import kotlin.math.abs
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
        rotor1 withSoftLanding {
            angle = 0
            duration = 2.seconds
        }
        rotor2 withSoftLanding {
            angle = 0
            duration = 3.seconds
        }
        rotor3 withSoftLanding {
            angle = 0
            duration = 2.seconds
        }
        rotor4 withSoftLanding {
            angle = 0
            duration = 2.seconds
        }
//        motorStepperRotator withSoftLanding {
//            angle = 0
//            duration = 5.seconds
//        }
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
            val timed = 6.seconds
            driveStepperRotator smoothly {
                angle = 90
                duration = timed
            }
            rotor1 withSoftLaunch {
                startDelay = 2.seconds
                angle = 25
                duration = 2.seconds
            }
            rotor2 withSoftLanding {
                startDelay = 3.seconds
                angle = 90
                duration = timed
            }
            rotor3 withSoftLanding {
                startDelay = 4.seconds
                angle = 90
                duration = timed
            }
            rotor4 withSoftLaunch {
                startDelay = 10.seconds
                angle = 180
                duration = 3.seconds
            }
        }

        MoveMessage {
            upScene()
            neoPixel[8] = WS2811.PixelColor(Color.GREEN, brightness = 0.1f)
            delay(2.seconds)
            neoPixel[8] = Color.BLACK
            home()
        }.publish()
    }

    fun frontThing() {
//        MoveMessage {
//            val elapsedMs = kotlin.system.measureTimeMillis { testIt() }
//            println("play took ${elapsedMs} ms")
//            sleep(500.milliseconds.inWholeMilliseconds)
//            parkIt()
//        }.publish()
    }

    fun everybodyAllAtOnce() {
        val fullScene = sceneBuilder {
            rotor1 withSoftLaunch {
                angle = 90
                duration = 6.seconds
            }
            rotor2 smoothly {
                startDelay = 1.seconds
                angle = 90
                duration = 3.seconds
            }
            rotor3 withSoftLanding {
                angle = 110
                duration = 4.seconds
            }
            rotor4 smoothly {
                angle = 90
                duration = 4.seconds
            }
//            motorStepperRotator withSoftLanding {
//                angle = 180
//                duration = 10.seconds
//            }
            driveStepperRotator withSoftLanding {
                angle = 90
                duration = 10.seconds
            }
        }
        MoveMessage {
            fullScene()
            delay(2.seconds)
            home()
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
