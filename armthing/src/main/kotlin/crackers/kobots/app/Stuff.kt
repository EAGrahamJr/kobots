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
import crackers.kobots.devices.expander.CRICKITHatDeviceFactory
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

// shared devices
internal val crickitHat by lazy { CRICKITHatDeviceFactory() }

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
