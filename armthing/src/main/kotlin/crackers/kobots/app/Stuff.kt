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

import com.diozero.util.SleepUtil
import com.typesafe.config.ConfigFactory
import crackers.hassk.HAssKClient
import crackers.kobots.app.bus.KobotsEvent
import crackers.kobots.devices.expander.CRICKITHat
import org.json.JSONObject
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

// shared devices
internal val crickitHat by lazy { CRICKITHat() }

// TODO this might be useful as a generic app convention?
// threads and execution control
internal val executor = Executors.newCachedThreadPool()
internal val runFlag = AtomicBoolean(true)

/**
 * Run a [block] and ensure it takes up **at least** [millis] time. This is basically to keep various parts from
 * overloading the various buses.
 */
internal fun <R> executeWithMinTime(millis: Long, block: () -> R): R {
    val startAt = System.currentTimeMillis()
    val response = block()
    val runtime = System.currentTimeMillis() - startAt
    if (runtime < millis) {
        SleepUtil.busySleep(Duration.ofMillis(millis - runtime).toNanos())
    }
    return response
}

/**
 * Run an execution loop until the run-flag says stop
 */
internal fun checkRun(ms: Long, block: () -> Unit): Future<*> = executor.submit {
    while (runFlag.get()) executeWithMinTime(ms) { block() }
}

/**
 * HomeAssistant client
 */
internal val hasskClient by lazy {
    with(ConfigFactory.load()) {
        HAssKClient(getString("ha.token"), getString("ha.server"), getInt("ha.port"))
    }
}

/**
 * Generic topic and event for sleep/wake events.
 */
internal const val SLEEP_TOPIC = "System.Sleep"

class SleepEvent(val sleep: Boolean) : KobotsEvent

/**
 * Extension function on a JSON object to get a on/off status as a boolean.
 */
internal fun JSONObject.onOff(key: String): Boolean = this.optString(key, "off") == "on"
