/*
 * Copyright 2022-2025 by E. A. Graham, Jr.
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

import crackers.kobots.parts.movement.ActionSequence
import crackers.kobots.parts.movement.DefaultActionSpeed
import crackers.kobots.parts.movement.action
import crackers.kobots.parts.movement.sequence
import crackers.kobots.app.mechanicals.SuzerainOfServos as suzi

/**
 * Pre-canned sequences, esp the home one.
 */
object Predestination {
    val gripperOpen = action { suzi.fingers goTo suzi.FINGERS_OPEN }
    val gripperClose = action { suzi.fingers goTo suzi.FINGERS_CLOSED }

    fun waist(where: Int) =
        action {
            suzi.waist rotate where
            requestedSpeed = DefaultActionSpeed.VERY_FAST
        }

    /**
     * Send it home - should be "0" state for servos.
     */
    val homeSequence: ActionSequence
        get() =
            sequence {
                name = "Home"

                // make sure not holding anything
                this += gripperOpen

                // since not entirely sure where everything is, back off
                action {
                    suzi.elbow.let { el ->
                        if (el.current != 0) {
                            val target = (el.current.toInt() + 10).coerceIn(suzi.ELBOW_HOME, suzi.ELBOW_MAX)
                            el rotate target
                        }
                    }
                    suzi.shoulder.let { sh ->
                        if (sh.current != 0) {
                            val target = (sh.current.toInt() - 10).coerceIn(suzi.SHOULDER_HOME, suzi.SHOULDER_MAX)
                            sh rotate target
                        }
                    }
                    suzi.wrist rotate suzi.WRIST_REST
                }

                // drop everything and twist home
                action {
                    suzi.elbow rotate suzi.ELBOW_HOME
                    suzi.shoulder rotate suzi.SHOULDER_HOME
                }
                this += waist(suzi.WAIST_HOME)
            }

    /**
     * Wave hello kinda.
     */
    val sayHi =
        sequence {
            name = "Say Hi"

            this += waist(135)
            action {
                suzi.elbow rotate suzi.ELBOW_MAX
                suzi.wrist rotate suzi.WRIST_COCKED
            }

            repeat(4) {
                action {
                    suzi.fingers goTo suzi.FINGERS_CLOSED
                    requestedSpeed = DefaultActionSpeed.FAST
                }
                action {
                    suzi.fingers goTo suzi.FINGERS_OPEN
                    requestedSpeed = DefaultActionSpeed.FAST
                }
            }
            this += homeSequence
        }

    /**
     * Does some crazy random shit, just because.
     */
    fun craCraSequence() =
        sequence {
            name = "CraCra"
        }

    val fourTwenty =
        sequence {
            name = "420"
        }

    val attackMode =
        sequence {
            action {
                requestedSpeed = DefaultActionSpeed.VERY_FAST
                suzi.shoulder rotate 60
                suzi.elbow rotate 40
                suzi.wrist rotate 90
                suzi.fingers goTo 50
            }
        }
}
