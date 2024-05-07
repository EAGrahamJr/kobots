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
        val swingDest: Int? = swing?.also { require(swing in Suzie.SWING_HOME..Suzie.SWING_MAX) }
        val boomDest: Int? = boom?.also { require(boom in Suzie.BOOM_HOME..Suzie.BOOM_DOWN) }
        val armDest: Int? = arm?.also { require(arm in Suzie.ARM_DOWN..Suzie.ARM_UP) }
        val bucketDest: Int? = bucket?.also { require(bucket in Suzie.BUCKET_HOME..Suzie.BUCKET_MAX) }

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
    ) {
        fun toAction() = action {
            with(location) {
                Suzie.let {
                    if (swingDest != null) it.swing rotate swingDest
                    if (boomDest != null) it.boomLink rotate boomDest
                    if (armDest != null) it.armLink rotate armDest
                    if (bucketDest != null) it.bucketLink rotate bucketDest
                    requestedSpeed = speed
                }
            }
        }
    }

    class DurationSpeed(d: Duration) : ActionSpeed {
        override val millis: Long = d.inWholeMilliseconds
    }

    val DEFAULT_GRAB = {
        KobotSleep.millis(200)
        true
    }

    /**
     * Move to a location and [squeeze] the gripper, with an optional [grabTrigger]
     * to initialize the gripper action.
     */
    fun grabFrom(where: ArmAction, squeeze: Int, grabTrigger: () -> Boolean = DEFAULT_GRAB): ActionSequence {
//        require(where.location.fullyQualified()) { "The location must be complete." }

        return sequence {
            warning("Starting grab")

            // go to a preliminary position that is just a little off
            action {
                with(where.location) {
                    Suzie.let {
                        if (swingDest != null) it.swing rotate swingDest
                        if (boomDest != null) it.boomLink rotate boomDest - 5
                        if (armDest != null) it.armLink rotate armDest - 5
                        if (bucketDest != null) it.bucketLink rotate bucketDest - 5
                        requestedSpeed = where.speed
                    }
                }
            }
            // creep up on it and get it
            action {
                requestedSpeed = DefaultActionSpeed.SLOW
                with(where.location) {
                    Suzie.let {
                        if (boomDest != null) it.boomLink rotate boomDest
                        if (armDest != null) it.armLink rotate armDest
                        if (bucketDest != null) it.bucketLink rotate bucketDest
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
