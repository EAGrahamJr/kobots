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

import crackers.kobots.app.SuzerainOfServos.primaryPivot
import crackers.kobots.app.SuzerainOfServos.wavyRotor
import crackers.kobots.devices.MG90S_TRIM
import crackers.kobots.parts.movement.ActionSpeed
import crackers.kobots.parts.movement.SequenceExecutor
import crackers.kobots.parts.movement.ServoRotator
import crackers.kobots.parts.movement.sequence

/**
 * All the things
 */
object SuzerainOfServos : SequenceExecutor("Suzie", AppCommon.mqttClient) {
    internal const val INTERNAL_TOPIC = "Servo.Suzie"

    override fun canRun() = AppCommon.applicationRunning
    val primaryPivot by lazy {
        val servo = hat.getServo(0, MG90S_TRIM, 0)
        // 1 to 1 gear ratio
        ServoRotator(servo, 0..90, 0..90)
    }
    val wavyRotor by lazy {
        val wavyServo = hat.getServo(1, MG90S_TRIM, 0)
        // 12/20 - 0.6 gear ratio
        ServoRotator(wavyServo, 0..90, 0..150)
    }

    const val SENSAI_MIN = -45
    const val SENSAI_MAX = 225
    val senseiServo by lazy { hat.getServo(2, MG90S_TRIM, 0) }
    val senseiRotor by lazy {
        // 1.67:1 gear ratio with full rotation, offset by -45 degrees
        ServoRotator(senseiServo, SENSAI_MIN..SENSAI_MAX, 0..180)
    }
}

val swirlyMax by lazy {
    sequence {
        name = "Swirly Max"
        action {
            primaryPivot rotate 90
            requestedSpeed = ActionSpeed.SLOW
        }
    }
}

val swirlyHome by lazy {
    sequence {
        name = "Swirly Home"
        action {
            primaryPivot rotate 0
            requestedSpeed = ActionSpeed.SLOW
        }
    }
}

val swirlyCenter by lazy {
    sequence {
        name = "Swirly Center"
        action {
            primaryPivot rotate 45
            requestedSpeed = ActionSpeed.SLOW
        }
    }
}

val wavyUp by lazy {
    sequence {
        name = "Wavy Up"
        action {
            wavyRotor rotate 90
            requestedSpeed = ActionSpeed.VERY_FAST
        }
    }
}

val wavyDown by lazy {
    sequence {
        name = "Wavy Down"
        action {
            wavyRotor rotate 0
            requestedSpeed = ActionSpeed.SLOW
        }
    }
}
