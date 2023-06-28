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

import crackers.kobots.app.arm.TheArm.SHOULDER_UP
import crackers.kobots.app.arm.armSequence
import java.time.Duration

/*
 * Demonstration type sequences for V3, and V4.
 */

/**
 * Pick up a LEGO tire from the starting point and deliver it to the exit point.
 */
val tireDance by lazy {
    val ELBOW_WHEEL = 90f
    val SHOULDER_WHEEL = 70f
    val SHOULDER_MIDMOVE = 110f
    val ELBOW_MIDMOVE = 45f
    val GRIPPER_GRAB = 20f
    val WAIST_HALFWAY = 45f
    val WAIST_ALLTHEWAY = 90f
    armSequence {
        home()
        movement { elbow { angle = ELBOW_MIDMOVE } }
        movement { shoulder { angle = SHOULDER_MIDMOVE } }
        gripperOpen()
        movement {
            shoulder { angle = SHOULDER_WHEEL }
            elbow { angle = ELBOW_WHEEL }
        }
        gripper(GRIPPER_GRAB)
        movement { shoulder { angle = SHOULDER_MIDMOVE } }
        movement {
            waist { angle = WAIST_HALFWAY }
            shoulder { angle = SHOULDER_UP }
            elbow { angle = ELBOW_MIDMOVE }
        }
        movement {
            waist { angle = WAIST_ALLTHEWAY }
            shoulder { angle = SHOULDER_MIDMOVE }
            elbow { angle = ELBOW_WHEEL }
        }
        movement { shoulder { angle = SHOULDER_WHEEL } }
        gripperOpen()
        movement { shoulder { angle = SHOULDER_MIDMOVE } }
        home()
    }
}

val sayHi by lazy {
    armSequence {
        home()
        movement {
            elbow {
                angle = 0f
            }
            gripper {
                angle = 70f
            }
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
