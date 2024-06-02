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

import crackers.kobots.parts.movement.sequence

/**
 * Ibid
 */
object CannedSequences {
    val home = sequence {
        action {
            Jimmy.sunElevation rotate 0
            Jimmy.sunAzimuth rotate 0
//            requestedSpeed = DefaultActionSpeed.SLOW
        }
    }

    val resetHome = sequence {
        this += home
        action {
            execute {
                Jimmy.sunAzimuth.apply {
                    reset()
                    release()
                }
                true
            }
        }
    }

    const val AZIMUTH_OFFSET = 15
    fun setSun(azimuth: Int, elevation: Int) =
        (azimuth + AZIMUTH_OFFSET).let { az ->
            if (elevation < 0 || az >= Jimmy.ABSOLUTE_AZIMUTH_LIMIT) home
            else sequence {
                action {
                    Jimmy.sunAzimuth rotate az
                    Jimmy.sunElevation rotate elevation
                }
            }
        }
}
