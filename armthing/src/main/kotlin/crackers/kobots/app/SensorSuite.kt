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

import crackers.kobots.app.arm.TheArm
import crackers.kobots.app.bus.KobotsEvent
import crackers.kobots.app.bus.allStop
import crackers.kobots.app.bus.publishToTopic
import crackers.kobots.app.execution.excuseMe
import crackers.kobots.app.execution.sayHi
import crackers.kobots.devices.sensors.VCNL4040
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.ln

/**
 * Simple proximity sensor with events and state.
 */
object SensorSuite : AutoCloseable {
    const val CLOSE_ENOUGH = 15 // this is **approximately**  25mm
    const val ALARM_TOPIC = "Prox.TooClose"
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

    class ProximityAlert : KobotsEvent
    class LumensData(val lumens: Int) : KobotsEvent

    private val ohCrap = ProximityAlert()
    private var previousLumenus: Int = 0

    fun start() {
        future = checkRun(10) {
            val reading = proximitySensor.proximity.toInt()
            proximity = reading
            if (tooClose) {
                publishToTopic(TheArm.REQUEST_TOPIC, allStop)
                publishToTopic(ALARM_TOPIC, ohCrap)
            }
            lumens.also { lumens ->
                if (abs(lumens - previousLumenus) > 2) {
                    publishToTopic(LUMEN_TOPIC, LumensData(lumens))
                    lumeAlerts(lumens)
                    previousLumenus = lumens
                }
            }
        }
    }

    private fun lumeAlerts(lumens: Int): Double {
        val l = lumens.toDouble()
        val p = previousLumenus.toDouble()
        val diff = abs((ln(l) - ln(p)) / ln(l))
        if (diff > .1) {
            when {
                l > 300 && previousLumenus < 300 -> TheArm.request(sayHi)
                l > 5 && previousLumenus < 5 -> TheArm.request(excuseMe)
            }
        }
        return l
    }

    override fun close() {
        future.get()
        proximitySensor.close()
    }
}
