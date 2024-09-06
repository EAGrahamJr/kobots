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

import crackers.kobots.app.dostuff.SuzerainOfServos.ELBOW_HOME
import crackers.kobots.app.dostuff.SuzerainOfServos.FINGERS_CLOSED
import crackers.kobots.app.dostuff.SuzerainOfServos.FINGERS_OPEN
import crackers.kobots.app.dostuff.SuzerainOfServos.PALM_DOWN
import crackers.kobots.app.dostuff.SuzerainOfServos.PALM_UP
import crackers.kobots.app.dostuff.SuzerainOfServos.SHOULDER_HOME
import crackers.kobots.app.dostuff.SuzerainOfServos.WAIST_HOME
import crackers.kobots.app.dostuff.SuzerainOfServos.elbow
import crackers.kobots.app.dostuff.SuzerainOfServos.fingers
import crackers.kobots.app.dostuff.SuzerainOfServos.shoulder
import crackers.kobots.app.dostuff.SuzerainOfServos.waist
import crackers.kobots.app.dostuff.SuzerainOfServos.wrist
import crackers.kobots.parts.movement.*

/**
 * Pre-canned sequences, esp the home one.
 */
object Predestination {
    val gripperOpen = sequence { action { fingers goTo FINGERS_OPEN } }


    /**
     * Send it home - should be "0" state for servos.
     */
    val homeSequence: ActionSequence
        get() = sequence {
            name = "Home"

            // since not entirely sure where everything is, move the elbow "neutral" position, and
            // then take care of the palm and fingers
            action { elbow rotate elbow.current() + 30 }
            action {
                wrist rotate PALM_DOWN
                fingers goTo FINGERS_OPEN
            }

            // drop everything and twist home
            action {
                elbow rotate ELBOW_HOME
                shoulder rotate SHOULDER_HOME
                waist rotate WAIST_HOME
            }
        }

    /**
     * Wave hello kinda.
     */
    val sayHi = sequence {
        name = "Say Hi"

        action {
            elbow rotate 45
            fingers goTo FINGERS_OPEN
            waist rotate 45
        }
        action {
            shoulder rotate 90
            elbow rotate 90
            waist rotate 90
        }
        action { wrist rotate PALM_UP / 2 }
        repeat(4) {
            action {
                fingers goTo FINGERS_CLOSED
                requestedSpeed = DefaultActionSpeed.FAST
            }
            action {
                fingers goTo FINGERS_OPEN
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

    }

    val fourTwenty = sequence {
        name = "420"

    }

    val attackMode = sequence {
        action {
            requestedSpeed = DefaultActionSpeed.VERY_FAST
            elbow rotate 40
            wrist rotate 90
            fingers goTo 50
        }
    }

    fun whipIt(target: Int, rotator: Rotator, speed: ActionSpeed = DefaultActionSpeed.SLOW) = sequence {
        name = "HA Request"
        action {
            rotator rotate target
            requestedSpeed = speed
        }
    }
}
