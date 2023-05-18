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

import kotlin.system.exitProcess

private val buttons by lazy { (1..4).toList().map { crickitHat.touchDigitalIn(it) } }


private val NO_BUTTONS = listOf(false, false, false, false)
private var lastButtonValues = NO_BUTTONS
private lateinit var currentButtons: List<Boolean>

// because we're looking for "presses", only return values when a value transitions _to_ true
private fun buttonCheck(): Boolean {
    currentButtons = buttons.map { it.value }.let { read ->
        // TODO add an elapsed time check?
        if (read == lastButtonValues) NO_BUTTONS
        else {
            lastButtonValues = read
            read
        }
    }
    return currentButtons.isEmpty() || !currentButtons[3]
}

const val WAIT_LOOP = 10L

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
        while (buttonCheck()) {
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
