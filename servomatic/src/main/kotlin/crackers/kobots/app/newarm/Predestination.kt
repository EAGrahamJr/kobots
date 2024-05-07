/*
 * Copyright 2022-2024 by E. A. Graham, Jr.
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

package crackers.kobots.app.newarm

import crackers.kobots.app.SuzerainOfServos.ARM_HOME
import crackers.kobots.app.SuzerainOfServos.ARM_UP
import crackers.kobots.app.SuzerainOfServos.BOOM_HOME
import crackers.kobots.app.SuzerainOfServos.BUCKET_HOME
import crackers.kobots.app.SuzerainOfServos.BUCKET_MAX
import crackers.kobots.app.SuzerainOfServos.GRIPPER_CLOSED
import crackers.kobots.app.SuzerainOfServos.GRIPPER_OPEN
import crackers.kobots.app.SuzerainOfServos.SWING_HOME
import crackers.kobots.app.SuzerainOfServos.SWING_MAX
import crackers.kobots.app.SuzerainOfServos.armLink
import crackers.kobots.app.SuzerainOfServos.boomLink
import crackers.kobots.app.SuzerainOfServos.bucketLink
import crackers.kobots.app.SuzerainOfServos.gripper
import crackers.kobots.app.SuzerainOfServos.swing
import crackers.kobots.parts.movement.DefaultActionSpeed
import crackers.kobots.parts.movement.sequence
import org.tinylog.kotlin.Logger
import kotlin.random.Random

/**
 * Pre-canned sequences, esp the home one.
 */
object Predestination {
    val gripperOpen = sequence { action { gripper goTo GRIPPER_OPEN } }

    /**
     * Make sure the gripper is out of the way of anything
     */
    val outOfTheWay = sequence {
        action { boomLink rotate BOOM_HOME }
        action { armLink rotate ARM_UP }
    }

    /**
     * Send it home - should be "0" state for servos.
     */
    val homeSequence = sequence {
        name = "Home"

        action {
            bucketLink rotate BUCKET_HOME
            gripper goTo GRIPPER_OPEN
        }
        action {
            boomLink rotate BOOM_HOME
            swing rotate SWING_HOME
        }
        action { armLink rotate ARM_HOME }
    }

    /**
     * Wave hello kinda.
     */
    val sayHi = sequence {
        name = "Say Hi"

        action {
            boomLink rotate 45
            gripper goTo GRIPPER_OPEN
            swing rotate 90
        }
        action { armLink rotate ARM_UP }
        (1..2).forEach {
            action { bucketLink rotate 180 }
            action { bucketLink rotate 0 }
        }
        action {
            armLink rotate 45
            bucketLink rotate 30
        }
        (1..4).forEach {
            action {
                gripper goTo GRIPPER_CLOSED
                requestedSpeed = DefaultActionSpeed.FAST
            }
            action {
                gripper goTo GRIPPER_OPEN
                requestedSpeed = DefaultActionSpeed.FAST
            }
        }
        this += homeSequence
    }

    /**
     * Does some crazy random shit, just because.
     */
    fun craCraSequence() = sequence {
        name = "CraCra"

        (1..5).forEach {
            // semi-random swing and arm
            action {
                swing rotate Random.nextInt(SWING_HOME, SWING_MAX)
                armLink rotate Random.nextInt(15, ARM_UP)
            }
            // boom separate
            action { boomLink rotate Random.nextInt(5, 35) }
            // spin the head a couple of times
            (1..3).forEach {
                action {
                    bucketLink rotate {
                        angle = Random.nextInt(BUCKET_HOME, BUCKET_MAX)
                        stopCheck = {
                            val current = boomLink.current()
                            (current !in 15..30).also {
                                if (it) Logger.warn("Yikes $current")
                            }
                        }
                    }
                }
            }
            (1..4).forEach {
                action {
                    gripper goTo GRIPPER_CLOSED
                    requestedSpeed = DefaultActionSpeed.FAST
                }
                action {
                    gripper goTo GRIPPER_OPEN
                    requestedSpeed = DefaultActionSpeed.FAST
                }
            }
        }

        this += outOfTheWay + homeSequence
    }

    val fourTwenty = sequence {
        name = "420"

        this += outOfTheWay
        action {
            bucketLink rotate 90
            gripper goTo 40
        }

        listOf(SWING_MAX, 90, 45).forEach { schwing ->
            action {
                requestedSpeed = DefaultActionSpeed.FAST
                swing rotate schwing
            }
            (1..4).forEach {
                action {
                    requestedSpeed = DefaultActionSpeed.FAST
                    armLink rotate ARM_UP - 30
                }
                action {
                    requestedSpeed = DefaultActionSpeed.FAST
                    armLink rotate ARM_UP
                }
            }
        }

        this += gripperOpen + homeSequence
    }

    val attackMode = sequence {
        action {
            requestedSpeed = DefaultActionSpeed.VERY_FAST
            boomLink rotate 40
            armLink rotate 90
            bucketLink rotate 90
            gripper goTo 50
        }
    }
}
