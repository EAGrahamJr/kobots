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

import crackers.kobots.app.arm.TheArm
import crackers.kobots.app.arm.armSequence
import java.time.Duration

/*
 * Demonstration type sequences.
 */

/**
 * Pick up a LEGO tire from the starting point and deliver it to the exit point.
 */
val tireDance by lazy {
    val ELBOW_MOVE = 30f
    val SHOULDER_DOWN = 10f
    val SHOULDER_MIDMOVE = 90f
    val GRIPPER_GRAB = 20f
    val WAIST_HALFWAY = 45f
    val WAIST_ALLTHEWAY = 90f
    armSequence {
        home()
        gripperOpen()
        movement {
            shoulder { angle = SHOULDER_DOWN }
            elbow { angle = ELBOW_MOVE }
        }
        gripper(GRIPPER_GRAB)
        movement {
            shoulder { angle = SHOULDER_MIDMOVE }
        }
        movement {
            waist { angle = WAIST_HALFWAY }
            shoulder { angle = TheArm.SHOULDER_UP }
            elbow { angle = TheArm.ELBOW_STRAIGHT }
        }
        movement {
            waist { angle = WAIST_ALLTHEWAY }
            shoulder { angle = SHOULDER_MIDMOVE }
            elbow { angle = ELBOW_MOVE }
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

/**
 * Uses a fixed starting location, picks up an object (LEGO tire) from that location
 * and deposits it on the proximity sensor places on an arc the arm can reach.
 */
val getTire by lazy {
    armSequence {
        home()
        // get tire
        movement {
            waist { angle = 90f }
            gripperOpen()
        }
        movement {
            shoulder { angle = 20f }
            elbow { angle = 30f }
        }
        gripper(20f)
        // move up a bit
        movement { elbow { angle = 40f } }
        movement {
            shoulder { angle = 90f }
            elbow { angle = TheArm.ELBOW_STRAIGHT }
        }
        // schwing
        movement {
            waist {
                angle = 0f
                // found it?
                stopCheck = {
                    ProximitySensor.proximity > 5
                }
            }
            pauseBetweenMoves = Duration.ofMillis(50)
        }

        // put it down "gently"
        movement {
            waist {
                angle = -2f
                relative = true
            }
            elbow { angle = 15f }
            shoulder {
                angle = 50f
                stopCheck = {
                    (ProximitySensor.proximity > 30f)
                }
            }
            pauseBetweenMoves = Duration.ofMillis(100)
        }
        gripperOpen()
        movement {
            shoulder {
                angle = 145f
            }
        }
        home()
    }
}
