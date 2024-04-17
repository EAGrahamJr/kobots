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

package crackers.kobots.app

import crackers.kobots.app.SuzerainOfServos.ARM_DOWN
import crackers.kobots.app.SuzerainOfServos.BOOM_UP
import crackers.kobots.app.SuzerainOfServos.BUCKET_HOME
import crackers.kobots.app.SuzerainOfServos.GRIPPER_CLOSED
import crackers.kobots.app.SuzerainOfServos.GRIPPER_OPEN
import crackers.kobots.app.SuzerainOfServos.SWING_HOME
import crackers.kobots.app.SuzerainOfServos.armLink
import crackers.kobots.app.SuzerainOfServos.boomLink
import crackers.kobots.app.SuzerainOfServos.bucketLink
import crackers.kobots.app.SuzerainOfServos.gripper
import crackers.kobots.app.SuzerainOfServos.swing
import crackers.kobots.parts.movement.DefaultActionSpeed
import crackers.kobots.parts.movement.sequence

/**
 * Pre-canned sequences, esp the home one.
 */
object Predestination {
    /**
     * Wave hello kinda.
     */
    val sayHi by lazy {
        sequence {
            name = "Say Hi"
            action {
                armLink rotate 45
                boomLink rotate 45
                gripper goTo GRIPPER_OPEN
                swing rotate 90
                bucketLink rotate 45
            }
            (1..4).forEach {
                action {
                    gripper goTo GRIPPER_CLOSED
                    bucketLink rotate 30
                    requestedSpeed = DefaultActionSpeed.FAST
                }
                action {
                    gripper goTo GRIPPER_OPEN
                    bucketLink rotate 45
                    requestedSpeed = DefaultActionSpeed.FAST
                }
            }
            this += homeSequence
        }
    }

    /**
     * Send it home - should be "0" state for servos.
     */
    val homeSequence by lazy {
        sequence {
            name = "Home"
            // make sure the gripper is out of the way of anything
            action {
                boomLink rotate BOOM_UP
                gripper goTo GRIPPER_OPEN
            }
            action {
                bucketLink rotate BUCKET_HOME
                armLink rotate ARM_DOWN + 10
                swing rotate SWING_HOME
            }
            action {
                armLink rotate ARM_DOWN
            }
        }
    }

}
