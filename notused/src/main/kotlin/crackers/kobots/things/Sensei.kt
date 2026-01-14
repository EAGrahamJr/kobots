/*
 * Copyright 2022-2026 by E. A. Graham, Jr.
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

package crackers.kobots.app.enviro

import crackers.kobots.app.AppCommon
import crackers.kobots.devices.sensors.VCNL4040
import crackers.kobots.parts.movement.async.AppScope
import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


object Sensei : AppCommon.Startable {
    private val logger = LoggerFactory.getLogger("Sensei")
    private val polly by lazy {
        VCNL4040().apply {
            ambientLightEnabled = true
            proximityEnabled = true
            proximityLEDCurrent = VCNL4040.LEDCurrent.LED_100MA
        }
    }

    val proximity: Int
        get() = polly.proximity.toInt()

    val ambientLight: Double
        get() = polly.ambientLight.toDouble()

    private lateinit var future: Job

    override fun start() {
        // initialize the sensors
        polly.proximity

        future =
            AppScope.scheduleWithFixedDelay(10.seconds, 1.minutes) {
                AppCommon.whileRunning {
                    HAStuff.ambientLightSensor.currentState = ambientLight
                }
            }
    }

    override fun stop() {
        if (::future.isInitialized) {
            future.cancel()
            polly.close()
        }

    }
}
