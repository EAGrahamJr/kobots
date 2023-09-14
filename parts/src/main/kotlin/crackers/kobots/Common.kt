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

import com.typesafe.config.ConfigFactory
import crackers.hassk.HAssKClient
import crackers.kobots.execution.executeWithMinTime
import org.json.JSONObject
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Whatever
 */
const val REMOTE_PI = "diozero.remote.hostname"
const val SERVO_TOPIC = "kobots/servoMatic"
const val EVENT_TOPIC = "kobots/events"

enum class ServoMaticCommand {
    STOP, UP, DOWN, LEFT, RIGHT, CENTER, SLEEP, WAKEY;
}

object AppCommon {
    // threads and execution control
    val executor by lazy { Executors.newScheduledThreadPool(4) }
    val runFlag = AtomicBoolean(true)

    /**
     * Run an execution loop until the run-flag says stop
     */
    fun checkRun(maxPause: Duration, block: () -> Unit): Future<*> = executor.submit {
        while (runFlag.get()) executeWithMinTime(maxPause) { block() }
    }

    /**
     * HomeAssistant client
     */
    val hasskClient by lazy {
        with(ConfigFactory.load()) {
            HAssKClient(getString("ha.token"), getString("ha.server"), getInt("ha.port"))
        }
    }

    /**
     * Generic topic and event for sleep/wake events.
     */
    const val SLEEP_TOPIC = "System.Sleep"

    class SleepEvent(val sleep: Boolean) : crackers.kobots.execution.KobotsEvent

    /**
     * Extension function on a JSON object to get a on/off status as a boolean.
     */
    fun JSONObject.onOff(key: String): Boolean = this.optString(key, "off") == "on"
}
