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

import crackers.kobots.app.HAJunk.tofSensor
import crackers.kobots.devices.sensors.VL6180X
import org.slf4j.LoggerFactory

object Sensei {
    private val logger = LoggerFactory.getLogger("Sensei")
    internal val toffle by lazy { VL6180X() }

    private fun VL6180X.distance(): Float = try {
        range.toFloat()
    } catch (e: Exception) {
        logger.error(e.localizedMessage)
        0f
    }

    fun publishEvent() {
        toffle.distance().run {
            if (this < 200) tofSensor.currentState = toString()
        }
    }
}
