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

package crackers.kobots.parts

import crackers.kobots.execution.*
import crackers.kobots.mqtt.KobotsMQTT
import crackers.kobots.utilities.KobotSleep
import org.json.JSONObject
import org.tinylog.Logger
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles running a sequence for a thing. Every sequence is executed on a background thread that runs until
 * completion or until the [stop] method is called or if an [EmergencyStop] is received. Only one sequence can be
 * running at a time (see the [moveInProgress] flag).
 */
abstract class SequenceExecutor(
    val executorName: String = this::class.java.simpleName,
    private val mqttClient: KobotsMQTT
) {
    private fun ActionSpeed.toMillis(): Long {
        return when (this) {
            ActionSpeed.SLOW -> 50
            ActionSpeed.NORMAL -> 15
            ActionSpeed.FAST -> 7
            else -> 15
        }
    }

    private val seqExecutor = Executors.newSingleThreadExecutor()

    /**
     * Execution control "lock".
     */
    private val _moving = AtomicBoolean(false)
    var moveInProgress: Boolean
        get() = _moving.get()
        private set(value) = _moving.set(value)

    private val _stop = AtomicBoolean(false)
    var stopImmediately: Boolean
        get() = _stop.get()
        private set(value) = _stop.set(value)

    class SequenceCompleted(val source: String, val sequence: String) : KobotsEvent

    /**
     * Sets the stop flag and blocks until the flag is cleared.
     */
    open fun stop() {
        stopImmediately = moveInProgress
        while (stopImmediately) KobotSleep.millis(5)
    }

    protected abstract fun canRun(): Boolean

    /**
     * Handles a request. If the request is a sequence, it is executed on a background thread. If the request is an
     * [EmergencyStop], the [stopImmediately] flag is set.
     *
     * This function is non-blocking.
     */
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
        if (!_moving.compareAndSet(false, true)) {
            Logger.warn("Sequence already running - rejected {}", request.sequence.name)
            return
        }
        seqExecutor.submit {
            preExecution()
            try {
                request.sequence.build().forEach { action ->
                    if (!canRun() || stopImmediately) return@forEach
                    var running = true
                    while (running) {
                        val maxPause = Duration.ofMillis(action.speed.toMillis())
                        executeWithMinTime(maxPause) {
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
            _stop.set(false) // clear emergency stop flag

            // publish completion event to the masses
            val completedMessage = SequenceCompleted(executorName, request.sequence.name)
            val payload = JSONObject(completedMessage).toString()
            mqttClient.publish(MQTT_TOPIC, payload)
            publishToTopic(INTERNAL_TOPIC, completedMessage)
        }
    }

    /**
     * Optional callback for pre-execution.
     */
    open fun preExecution() {}

    /**
     * Optiional callback for post-execution.
     */
    open fun postExecution() {}

    /**
     * Optionally updates the state of the executor.
     */
    open fun updateCurrentState() {}

    companion object {
        const val INTERNAL_TOPIC = "Executor.Sequences"
        const val MQTT_TOPIC = "kobots/events"
    }
}
