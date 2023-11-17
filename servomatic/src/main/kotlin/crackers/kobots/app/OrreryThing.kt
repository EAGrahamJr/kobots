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

import crackers.kobots.parts.movement.ActionSpeed
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

/**
 * TODO fill this in
 */
object OrreryThing {
    /**
     * Show the sun's elevation on the orrery: 180 on the rotator is 0 degrees elevation and 0 is 180 degrees elevation
     */
    private val elevation = AtomicReference(0f)
    internal var sunElevation: Float
        get() = elevation.get()
        set(value) {
            // in this case, + or - indicates rising or setting
            val next = when {
                value > 0 -> {
                    90 + (3 * (25 + value))
                }

                value < 0 -> {
                    90 - (3 * (25 - value))
                }

                else -> 0f
            }
            elevation.set(next.coerceIn(0f, 180f))
            servoRequest(orrerySun)
        }

    val orrerySun by lazy {
        crackers.kobots.parts.movement.sequence {
            name = "Orrery Sun"
            action {
                SuzerainOfServos.orreryRotor rotate sunElevation.roundToInt()
                requestedSpeed = ActionSpeed.SLOW
            }
        }
    }

}
