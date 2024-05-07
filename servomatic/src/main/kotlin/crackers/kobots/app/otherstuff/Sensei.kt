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

package crackers.kobots.app.otherstuff

import crackers.kobots.app.AppCommon
import crackers.kobots.app.HAJunk
import crackers.kobots.app.I2CFactory
import crackers.kobots.app.Startable
import crackers.kobots.devices.sensors.VCNL4040
import crackers.kobots.devices.sensors.VL6180X
import crackers.kobots.parts.elapsed
import crackers.kobots.parts.scheduleWithDelay
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.Future
import java.util.concurrent.ScheduledFuture
import kotlin.time.Duration.Companion.seconds

object Sensei : Startable {
    private val logger = LoggerFactory.getLogger("Sensei")
    private val toffle by lazy { VL6180X(I2CFactory.toffleDevice) }

    private fun VL6180X.distance(): Float = try {
        range.toFloat()
    } catch (e: Exception) {
        logger.error(e.localizedMessage)
        0f
    }

    private const val PROXIMITY_THRESHOLD = 4
    private val TRIP_DURATION = java.time.Duration.ofSeconds(2)
    private val polly by lazy {
        VCNL4040(I2CFactory.proxyDevice).apply {
            ambientLightEnabled = true
            proximityEnabled = true
        }
    }
    private var proxFiredAt = Instant.EPOCH

    private lateinit var future: Future<*>
    private lateinit var slowFuture: ScheduledFuture<*>

    override fun start() {
        // initialize the sensor
        toffle.range

        var lastProxTriggered = false
        future = AppCommon.executor.scheduleWithDelay(1.seconds) {
            AppCommon.whileRunning {
                // time-of-flight
                toffle.distance().run {
                    if (this < 200) HAJunk.tofSensor.currentState = toString()
                }

                // proximity
                polly.proximity.toInt().let {
                    (it > PROXIMITY_THRESHOLD).let { tripped ->
                        if (tripped) {
                            // fresh trip, start the countdown
                            if (!lastProxTriggered) {
                                proxFiredAt = Instant.now()
                            } else {
                                if (proxFiredAt.elapsed() > TRIP_DURATION) {
                                    fireProximityThings()
                                }
                            }
                        } else {
                            HAJunk.proxSensor.currentState = false
                        }
                        lastProxTriggered = tripped
                    }
                }
            }
        }

        slowFuture = AppCommon.executor.scheduleWithDelay(30.seconds) {
            AppCommon.whileRunning {
                HAJunk.ambientSensor.currentState = polly.luminosity.toString()
            }
        }
    }

    private fun fireProximityThings() {
        HAJunk.proxSensor.currentState = true
    }

    override fun stop() {
        Sensei::slowFuture.isInitialized && slowFuture.cancel(true)
        Sensei::future.isInitialized && future.cancel(true)
    }
}
