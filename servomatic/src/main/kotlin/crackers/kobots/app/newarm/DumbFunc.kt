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

import crackers.kobots.parts.app.KobotSleep
import crackers.kobots.parts.movement.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import crackers.kobots.app.SuzerainOfServos as Suzie

/**
 * Simple functionality, not quite canned motions.
 */
object DumbFunc {
    private val logger = LoggerFactory.getLogger("DumbFunc")

    class ArmLocation(swing: Int? = null, boom: Int? = null, arm: Int? = null, bucket: Int? = null) {
        val swingDest: Int?
        val boomDest: Int?
        val armDest: Int?
        val bucketDest: Int?

        init {
            swingDest = swing?.also { require(swing in Suzie.SWING_HOME..Suzie.SWING_MAX) }
            boomDest = boom?.also { require(boom in Suzie.BOOM_HOME..Suzie.BOOM_DOWN) }
            armDest = arm?.also { require(arm in Suzie.ARM_DOWN..Suzie.ARM_UP) }
            bucketDest = bucket?.also { require(bucket in Suzie.BUCKET_HOME..Suzie.BUCKET_MAX) }
        }

        /**
         * Set the bucket position based on where this location is. [flipped] is only valid when [horizontal] is true,
         * based on the limitations of the device.
         */
        fun setBucketPosition(horizontal: Boolean = true, flipped: Boolean = false) {
            TODO("Calcuate where the bucket should be")
        }


        fun fullyQualified() = swingDest != null && boomDest != null && armDest != null && bucketDest != null
    }

    class ArmAction(
        val location: ArmLocation,
        val speed: ActionSpeed = DefaultActionSpeed.NORMAL
    )

    class DurationSpeed(d: Duration) : ActionSpeed {
        override val millis: Long = d.inWholeMilliseconds
    }

    val DEFAULT_GRAB = {
        KobotSleep.millis(200)
        true
    }

    /**
     * Move to a locaiton and [squeeze] the gripper, with an optional [grabTrigger]
     * to initialize the gripper action.
     */
    fun grabFrom(where: ArmAction, squeeze: Int, grabTrigger: () -> Boolean = DEFAULT_GRAB): ActionSequence {
        require(where.location.fullyQualified()) { "The location must be complete." }

        return sequence {
            warning("Starting grab")

            // first, make sure not to hit anything
            this += Predestination.outOfTheWay

            // go to a preliminary position that is just a little off
            action {
                with(where.location) {
                    Suzie.let {
                        it.swing rotate swingDest!!
                        it.boomLink rotate boomDest!! - 5
                        it.armLink rotate armDest!! - 5
                        it.bucketLink rotate bucketDest!! - 5
                        requestedSpeed = where.speed
                    }
                }
            }
            // creep up on it and get it
            action {
                requestedSpeed = DefaultActionSpeed.SLOW
                with(where.location) {
                    Suzie.let {
                        it.boomLink rotate boomDest!!
                        it.armLink rotate armDest!!
                        it.bucketLink rotate bucketDest!!
                    }
                }
            }
            action {
                execute(grabTrigger)
            }
            action {
                Suzie.gripper goTo squeeze
            }
        }
    }

    private fun warning(msg: String, vararg args: Any) = action {
        execute {
            logger.warn(msg, args)
            true
        }
    }


}
