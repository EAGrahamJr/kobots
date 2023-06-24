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

import crackers.kobots.app.arm.TheArm.SHOULDER_DOWN
import crackers.kobots.app.arm.TheArm.SHOULDER_UP
import crackers.kobots.app.arm.armSequence
import java.time.Duration

/*
 * Demonstration type sequences.
 */

/**
 * Pick up a LEGO tire from the starting point and deliver it to the exit point.
 */
val tireDance by lazy {
    val ELBOW_WHEEL = 130f
    val SHOULDER_MIDMOVE = 150f
    val ELBOW_MIDMOVE = 45f
    val ELBOW_MIDWAY = 90f
    val GRIPPER_GRAB = 20f
    val WAIST_HALFWAY = 45f
    val WAIST_ALLTHEWAY = 90f
    armSequence {
        home()
        gripperOpen()
        movement {
            shoulder { angle = SHOULDER_MIDMOVE }
            elbow { angle = ELBOW_MIDMOVE }
        }
        movement {
            shoulder { angle = SHOULDER_DOWN }
            elbow { angle = ELBOW_WHEEL }
        }
        gripper(GRIPPER_GRAB)
        movement {
            shoulder { angle = SHOULDER_MIDMOVE }
        }
        movement {
            waist { angle = WAIST_HALFWAY }
            shoulder { angle = SHOULDER_UP }
            elbow { angle = ELBOW_MIDWAY }
        }
        movement {
            waist { angle = WAIST_ALLTHEWAY }
            shoulder { angle = SHOULDER_MIDMOVE }
            elbow { angle = ELBOW_WHEEL }
        }
        movement {
            shoulder { angle = SHOULDER_DOWN }
        }
        gripperOpen()
        movement {
            shoulder { angle = SHOULDER_MIDMOVE }
        }
        home()
    }
}

val downAndOut by lazy {
    armSequence {
        home()
        movement {
            shoulder { angle = 45f }
            elbow { angle = 45f }
        }
        movement {
            pauseBetweenMoves = Duration.ofSeconds(2)
        }
        home()
    }
}

val sayHi by lazy {
    armSequence {
        home()
        movement {
            elbow {
                angle = 0f
                pauseBetweenMoves = Duration.ofMillis(1)
            }
            gripper {
                angle = 70f
            }
        }
        (1..4).forEach {
            movement {
                waist {
                    angle = 90f
                }
            }
            movement {
                waist {
                    angle = 0f
                }
            }
        }
        home()
    }
}
