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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

// devices
internal val crickitHat by lazy { CRICKITHatDeviceFactory() }

private val buttons by lazy {
    (1..4).toList().map { crickitHat.touchDigitalIn(it) }
}

private val NO_BUTTONS = listOf(false, false, false, false)
private var lastButtonValues = NO_BUTTONS

// because we're looking for "presses", only return values when a value transitions _to_ true
private fun buttonCheck(): List<Boolean> {
    val currentButtons = buttons.map { it.value }
    // TODO add an elapsed time check?
    if (currentButtons == lastButtonValues) return NO_BUTTONS
    lastButtonValues = currentButtons
    return currentButtons
}

const val WAIT_LOOP = 10L

// threads and execution control
internal val executor = Executors.newCachedThreadPool()
private val runFlag = AtomicBoolean(true)

private fun checkRun(block: () -> Unit) {
    executor.submit {
        while (runFlag.get()) block()
    }
}

/**
 * Run this.
 */
fun main() {
    checkRun {
        executeWithMinTime(20) { Stripper.execute() }
    }
    checkRun {
        executeWithMinTime(10) { TheScreen.execute() }
    }

    crickitHat.use { hat ->
        // main loop!!!!!
        lateinit var currentButtons: List<Boolean>
        while (buttonCheck().let {
                currentButtons = it
                currentButtons.isEmpty() || !currentButtons[3]
            }
        ) {
            executeWithMinTime(WAIT_LOOP) {
                // figure out if we're doing anything
                val doThisStuff = ControlThing.execute(currentButtons)
                // TODO need to pub/sub here
                TheArm.execute(doThisStuff.armRequest)
            }
        }
        runFlag.set(false)

        ControlThing.close()
        TheArm.close()
        TheScreen.close()
    }
    executor.shutdownNow()
    exitProcess(0)
}

/**
 * Run a [block] and ensure it takes up **at least** [millis] time. This is basically to keep various parts from
 * overloading the various buses.
 */
fun <R> executeWithMinTime(millis: Long, block: () -> R): R {
    val startAt = System.currentTimeMillis()
    val response = block()
    val runtime = System.currentTimeMillis() - startAt
    if (runtime < millis) {
        SleepUtil.busySleep(Duration.ofMillis(millis - runtime).toNanos())
    }
    return response
}
