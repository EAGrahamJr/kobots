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
import crackers.kobots.devices.sensors.VCNL4040
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

/**
 * Simple proximity sensor with events and state.
 */
object ProximitySensor : AutoCloseable {
    const val CLOSE_ENOUGH = 15 // this is **approximately**  25mm
    const val ALARM_TOPIC = "Prox.TooClose"

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

    private val ohCrap = ProximityAlert()
    private var previousLumenus: Int = 0

    fun start() {
        future = checkRun(10) {
            val reading = proximitySensor.proximity.toInt()
            proximity = reading
            if (tooClose) publishToTopic(ALARM_TOPIC, ohCrap)
            lumens.also { l ->
                if (l > 100 && previousLumenus < 100) publishToTopic(TheArm.REQUEST_TOPIC, sayHi)
                previousLumenus = l
            }
        }
    }

    override fun close() {
        future.cancel(true)
        future.get()
        proximitySensor.close()
    }
}
