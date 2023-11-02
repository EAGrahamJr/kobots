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

import com.diozero.api.ServoTrim
import crackers.kobots.app.SuzerainOfServos.primaryPivot
import crackers.kobots.app.SuzerainOfServos.thingieSwirlie
import crackers.kobots.parts.app.KobotsAction
import crackers.kobots.parts.app.KobotsSubscriber
import crackers.kobots.parts.app.joinTopic
import crackers.kobots.parts.movement.ActionSpeed
import crackers.kobots.parts.movement.SequenceExecutor
import crackers.kobots.parts.movement.ServoRotator
import crackers.kobots.parts.movement.sequence

/**
 * All the things
 */
object SuzerainOfServos : SequenceExecutor("Suzie", AppCommon.mqttClient) {
    const val SERVO_TOPIC = "Servo.Suzie"

    override fun canRun() = AppCommon.applicationRunning
    val primaryPivot by lazy {
        val servo = hat.getServo(0, ServoTrim.TOWERPRO_SG90, 0)
        ServoRotator(servo, 0..180, 0..180)
    }
    val thingieSwirlie by lazy {
        val servo = hat.getServo(1, ServoTrim.TOWERPRO_SG90, 0)
        ServoRotator(servo, 0..140, 0..180)
    }

    init {
        joinTopic(SERVO_TOPIC, KobotsSubscriber<KobotsAction> { handleRequest(it) })
    }

}

val steveTurns by lazy {
    sequence {
        name = "Steve Turns"
        action {
            primaryPivot rotate 180
            requestedSpeed = ActionSpeed.VERY_FAST
        }
    }
}

val steveGoesHome by lazy {
    sequence {
        name = "Steve Goes Home"
        action {
            primaryPivot rotate 0
        }
    }
}

val swirlyMax by lazy {
    sequence {
        name = "Swirly Max"
        action {
            thingieSwirlie rotate 140
        }
    }
}

val swirlyHome by lazy {
    sequence {
        name = "Swirly Home"
        action {
            thingieSwirlie rotate 0
        }
    }
}
