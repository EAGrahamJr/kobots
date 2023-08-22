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

import crackers.kobots.devices.sensors.VCNL4040
import crackers.kobots.execution.KobotsEvent
import crackers.kobots.execution.publishToTopic
import crackers.kobots.utilities.KobotSleep
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.ln

/**
 * Simple proximity sensor with events and state.
 */
object SensorSuite : AutoCloseable {
    const val CLOSE_ENOUGH = 15 // this is **approximately**  25mm
    const val PROXIMITY_TOPIC = "Prox.TooClose"
    const val LUMEN_TOPIC = "Prox.Lumens"

    private lateinit var future: Future<*>

    private val proximitySensor by lazy {
        VCNL4040().apply {
            proximityEnabled = true
            ambientLightEnabled = true
        }
    }

    private val proximityReading = AtomicInteger(0)

    var proximity: Int
        get() = proximityReading.get()
        private set(i) {
            proximityReading.set(i)
        }

    val lumens: Int
        get() = proximitySensor.luminosity.toInt()

    val tooClose: Boolean
        get() = proximity > CLOSE_ENOUGH

    data class ProximityTrigger(val source: String = "proximity", val reading: Int) : KobotsEvent
    class LumensData(val source: String = "proximity", val lumens: Int) : KobotsEvent

    //    private val ohCrap = ProximityTrigger()
    private var previousLumenus: Int = 0
    private val logger = LoggerFactory.getLogger("SensorSuite")
    private val executor by lazy { Executors.newSingleThreadExecutor() }

    fun start() {
        future = executor.submit {
            while (doneLatch.count > 0) {
                val reading = proximitySensor.proximity.toInt()
                proximity = reading
                if (tooClose) {
                    logger.error("Proximity sensor triggered")
                    val trigger = ProximityTrigger(reading = reading)
                    publishToTopic(PROXIMITY_TOPIC, trigger)
                    mqttClient.publish(EVENT_TOPIC, JSONObject(trigger).toString())
                }
                lumens.also { lumens ->
                    if (abs(lumens - previousLumenus) > 2) {
                        val trigger = LumensData(lumens = lumens)
                        publishToTopic(LUMEN_TOPIC, trigger)
                        mqttClient.publish(EVENT_TOPIC, JSONObject(trigger).toString())
                        previousLumenus = lumens
                    }
                }
                KobotSleep.millis(20)
            }
        }
    }

    private fun lumeAlerts(lumens: Int): Double {
        val l = lumens.toDouble()
        val p = previousLumenus.toDouble()
        val diff = abs((ln(l) - ln(p)) / ln(l))
//        if (diff > .1) {
//            when {
//                l > 300 && previousLumenus < 300 -> TheArm.request(sayHi)
//                l > 5 && previousLumenus < 5 -> TheArm.request(excuseMe)
//            }
//        }
        return l
    }

    override fun close() {
        if (!SensorSuite::future.isInitialized) return
        executor.shutdownNow()
        proximitySensor.close()
    }
}
