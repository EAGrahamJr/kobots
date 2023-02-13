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

package dork

import com.diozero.devices.HCSR04
import crackers.kobots.devices.at
import crackers.kobots.devices.set
import crackers.kobots.utilities.SimpleAverageMeasurement
import crackers.kobots.utilities.withDelay
import java.time.LocalTime
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * CB1 configuration with multiple threads reading the sensors
 */
private val running = AtomicBoolean(true)
val threadPool = Executors.newScheduledThreadPool(5)

fun main() {
    Runtime.getRuntime().addShutdownHook(
        thread(start = false) {
            threadPool.shutdownNow()
            running.set(false)
        }
    )

    servo at 0f
    threadPool.withDelay(10.seconds, 100.milliseconds, ::servoReact)
    threadPool.withDelay(2.seconds, 750.milliseconds) { led set irSensor.value }
    threadPool.withDelay(Duration.ZERO, 250.milliseconds, ::displayThing)
}

private val isQuiet = AtomicBoolean(false)

fun displayThing() {
    val time = LocalTime.now()
    isQuiet.set(time.quietTime())
    if (!isQuiet.get()) {
        if (!LCD.isBacklightEnabled) LCD.isBacklightEnabled = true

        showTime(time)
        showTemp()

        val distanceCm = rangeFinder.distance.value
        LCD[3] = "Dist: ${distanceCm.toInt()} cm    " + if (distanceCm < 10f) 2.toChar() else ' '
    }
}

// react to distance if "too close" otherwise, hour of day-ish
fun servoReact() {
    if (!isQuiet.get()) {
        val distanceCm = rangeFinder.distance.value
        servo at if (distanceCm < 20f) 90f else (LocalTime.now().hour.toFloat() / 24f) * 180f
    }
}

val HCSR04.distance by lazy {
    // average over the last 10 ms
    SimpleAverageMeasurement(10).also {
        threadPool.withDelay(1.milliseconds, 1.milliseconds) { it += rangeFinder.distanceCm }
    }
}
