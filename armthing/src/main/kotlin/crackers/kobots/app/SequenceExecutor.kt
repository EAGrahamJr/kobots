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

import crackers.kobots.app.bus.EmergencyStop
import crackers.kobots.app.bus.KobotsAction
import crackers.kobots.app.bus.SequenceRequest
import crackers.kobots.parts.ActionSpeed
import crackers.kobots.utilities.KobotSleep
import org.tinylog.Logger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles running a sequence for a thing.
 */
abstract class SequenceExecutor {
    private fun ActionSpeed.toMillis(): Long {
        return when (this) {
            ActionSpeed.SLOW -> 50
            ActionSpeed.NORMAL -> 15
            ActionSpeed.FAST -> 7
            else -> 15
        }
    }

    // do stuff =======================================================================================================
    private val _moving = AtomicBoolean(false)
    var moveInProgress: Boolean
        get() = _moving.get()
        private set(value) = _moving.set(value)

    private val _stop = AtomicBoolean(false)
    var stopImmediately: Boolean
        get() = _stop.get()
        private set(value) = _stop.set(value)

    /**
     * Sets the stop flag and waits for it to clear.
     */
    open fun stop() {
        if (moveInProgress) stopImmediately = true
        while (stopImmediately) KobotSleep.millis(5)
    }

    private fun canRun() = runFlag.get() && !stopImmediately

    open fun handleRequest(request: KobotsAction) {
        when (request) {
            is EmergencyStop -> stopImmediately = true
            is SequenceRequest -> executeSequence(request)
            else -> {}
        }
    }

    /**
     * Executes the sequence in a separate thread. N.B. the sequence should only contain the devices/actions that
     * this handler can control.
     */
    protected fun executeSequence(request: SequenceRequest) {
        // claim it for ourselves and then use that for loop control
        if (!_moving.compareAndSet(false, true)) return
        preExecution()
        executor.submit {
            try {
                request.sequence.build().forEach { action ->
                    if (!canRun()) return@forEach
                    var running = true
                    while (running) {
                        executeWithMinTime(action.speed.toMillis()) {
                            running = action.action.step()
                        }
                        updateCurrentState()
                    }
                }
            } catch (e: Exception) {
                Logger.error("Error executing sequence", e)
            }
            postExecution()

            // done
            _moving.set(false)
            updateCurrentState()
            _stop.set(false)    // clera emergency stop flag
        }
    }

    protected abstract fun preExecution()
    protected abstract fun postExecution()

    /**
     * Updates the state of the arm, which in turn publishes to the event topic.
     */
    abstract fun updateCurrentState()
}
