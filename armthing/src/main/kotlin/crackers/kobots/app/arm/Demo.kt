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

package crackers.kobots.app.arm

import crackers.kobots.app.arm.TheArm.ELBOW_DOWN
import crackers.kobots.app.arm.TheArm.ELBOW_UP
import crackers.kobots.app.arm.TheArm.EXTENDER_OUT
import java.time.Duration

/*
 * Demonstration type sequences for V3, and V4.
 */

/**
 * Pick up something from the starting point and deliver it to the exit point.
 */
val pickAndMove by lazy {
    val EXTENDER_MIDMOVE = 90f
    val ELBOW_MIDMOVE = 90f
    val GRIPPER_GRAB = 50f
    val WAIST_HALFWAY = 45f
    val WAIST_ALLTHEWAY = 90f
    armSequence {
        home()
        movement {
            elbow { angle = ELBOW_MIDMOVE }
            extender { angle = EXTENDER_MIDMOVE }
        }
        gripperOpen()
        movement {
            extender { angle = EXTENDER_OUT }
            elbow { angle = ELBOW_DOWN }
        }
        gripper(GRIPPER_GRAB)
        movement { elbow { angle = ELBOW_MIDMOVE } }
        movement {
            waist { angle = WAIST_HALFWAY }
            extender { angle = EXTENDER_MIDMOVE }
        }
        movement { elbow { angle = ELBOW_UP } }
        movement {
            waist { angle = WAIST_ALLTHEWAY }
            elbow { angle = ELBOW_MIDMOVE }
        }
        movement {
            extender { angle = 135f }
            elbow { angle = ELBOW_DOWN }
        }
        gripperOpen()
        movement { elbow { angle = ELBOW_MIDMOVE } }
        home()
    }
}

val sayHi by lazy {
    armSequence {
        home()
        movement {
            elbow { angle = 45f }
            extender { angle = EXTENDER_OUT }
            gripperOpen()
            waist {
                angle = 90f
            }
        }
        (1..4).forEach {
            movement {
                waist {
                    angle = 105f
                }
                pauseBetweenMoves = Duration.ofMillis(5)
            }
            movement {
                waist {
                    angle = 75f
                }
                pauseBetweenMoves = Duration.ofMillis(5)
            }
        }
        home()
    }
}
