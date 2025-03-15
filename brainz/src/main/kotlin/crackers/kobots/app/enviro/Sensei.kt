/*
 * Copyright 2022-2025 by E. A. Graham, Jr.
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
import crackers.kobots.devices.sensors.VL6180X
import crackers.kobots.parts.scheduleWithDelay
import org.slf4j.LoggerFactory
import java.util.concurrent.Future
import kotlin.time.Duration.Companion.seconds

object Sensei : AppCommon.Startable {
    private val logger = LoggerFactory.getLogger("Sensei")
    private val toffle by lazy { VL6180X() }
    private val polly by lazy {
        VCNL4040().apply {
            ambientLightEnabled = true
            proximityEnabled = true
            proximityLEDCurrent = VCNL4040.LEDCurrent.LED_100MA
        }
    }

    val distance: Int
        get() = try {
            toffle.range
        } catch (e: Exception) {
            logger.error(e.localizedMessage)
            0
        }

    val proximity: Int
        get() = polly.proximity.toInt()

    val ambientLight: Int
        get() = polly.ambientLight.toInt()

    private lateinit var future: Future<*>

    override fun start() {
        // initialize the sensors
        toffle.range
        polly.proximity

        future =
            AppCommon.executor.scheduleWithDelay(1.seconds) {
                AppCommon.whileRunning {
                    // proximity sensor
                    proximity.run {
                        HAStuff.proxSensor.currentState = toString()
                    }
                }
            }
    }

    override fun stop() {
        Sensei::future.isInitialized && future.cancel(true)
    }
}
