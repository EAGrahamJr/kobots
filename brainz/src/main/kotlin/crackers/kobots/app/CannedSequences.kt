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
            duration = 2.seconds
        }
    }

    fun goHome() {
        runBlocking {
            home()
        }
    }

    fun wave420() {
        val turnMe = sceneBuilder {
            driveStepperRotator smoothly {
                angle = 90
                duration = 2.seconds // going to be longer than that
            }
        }

        val upScene = sceneBuilder {
            val d = 2.seconds
            defaultDuration = d
            rotor1 withSoftLaunch {
                angle = 25
            }
            rotor2 withSoftLanding {
                angle = rotor2.physicalRange.last
            }
            rotor3 withSoftLanding {
                angle = 45
            }
            rotor4 withSoftLaunch {
                startDelay = d
                angle = 180
                duration = 3.seconds
                endDelay = d
            }
        }

        val helloScene = sceneBuilder {
            defaultDuration = 1.seconds
            repeat(3) {
                rotor4 withSoftLanding {
                    angle = 0
                }
                rotor4 withSoftLanding {
                    angle = 90
                }
            }
        }

        MoveMessage {
            turnMe()
            upScene()
            helloScene()
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
                angle = 45
                duration = 6.seconds
            }
            rotor2 smoothly {
                startDelay = 1.seconds
                angle = 145
                duration = 3.seconds
            }
            rotor3 withSoftLanding {
                angle = 90
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
                duration = 5.seconds
            }
        }
        MoveMessage {
            fullScene()
            delay(2.seconds)
            home()
        }.publish()
    }

    fun movementThing() {
        val twoSec = 2.seconds

        val setup = sceneBuilder {
            defaultDuration = 1.seconds
            rotor1 moveTo {
                angle = 0
            }
            rotor2 moveTo {
                angle = rotor2.physicalRange.last
            }
        }

        val curtainUp = sceneBuilder {
            defaultDuration = twoSec
            rotor1 withSoftLaunch {
                angle = rotor1.physicalRange.last
            }
            rotor2 withSoftLanding {
                angle = 0
            }
        }

        val spotlightTime = 3.seconds
        val spotlighSweep1 = sceneBuilder {
            defaultDuration = spotlightTime
            rotor1 withSoftLaunch {
                angle = 35
            }
            rotor2 withSoftLanding {
                angle = 135
            }
        }
        val spotlighSweep2 = sceneBuilder {
            defaultDuration = spotlightTime
            rotor1 withSoftLaunch {
                angle = rotor1.physicalRange.last
            }
            rotor2 withSoftLanding {
                angle = 45
            }
        }
        val beDone = sceneBuilder {
            defaultDuration = 2.seconds
            rotor1 moveTo {
                angle = rotor1.physicalRange.last
            }
            rotor2 moveTo {
                angle = rotor2.physicalRange.last
            }
        }

        MoveMessage {
            +setup
            +curtainUp
            delay(5.seconds)
            repeat(3) {
                +spotlighSweep1
                delay(twoSec)
                +spotlighSweep2
            }
            +beDone
            delay(twoSec)
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
