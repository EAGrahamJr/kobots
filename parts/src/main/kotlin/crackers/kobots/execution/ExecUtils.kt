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

package crackers.kobots.execution

import com.diozero.util.SleepUtil
import java.time.Duration

/**
 * Run a [block] and ensure it takes up **at least** [maxPause] time. This is basically to keep various parts from
 * overloading the various buses.
 *
 * The purported granularity of the pause is _ostensibly_ nanoseconds.
 */
fun <R> executeWithMinTime(maxPause: Duration, block: () -> R): R {
    val pauseForNanos = maxPause.toNanos()
    val startAt = System.nanoTime()

    val response = block()

    val runtime = System.nanoTime() - startAt
    if (runtime < pauseForNanos) SleepUtil.busySleep(pauseForNanos - runtime)
    return response
}
