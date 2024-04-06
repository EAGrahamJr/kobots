/*
 * Copyright 2022-2024 by E. A. Graham, Jr.
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

package crackers.kobots.app.arm

import crackers.kobots.app.AppCommon
import crackers.kobots.app.Startable
import crackers.kobots.app.enviro.DieAufseherin
import crackers.kobots.app.enviro.HAStuff
import crackers.kobots.parts.app.publishToTopic
import crackers.kobots.parts.movement.ActionSequence
import crackers.kobots.parts.movement.SequenceExecutor
import crackers.kobots.parts.movement.SequenceRequest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * V7 iteration of a controlled "arm-like" structure. This is somewhat modelled after a backhoe arm
 */
object TheArm : SequenceExecutor("TheArm", AppCommon.mqttClient), Startable {
    const val REQUEST_TOPIC = "TheArm.Request"

    fun request(sequence: ActionSequence) {
        publishToTopic(REQUEST_TOPIC, SequenceRequest(sequence))
    }

    override fun canRun(): Boolean {
        TODO("Not yet implemented")
    }

    // manage the state of this construct =============================================================================
    private val _currentState = AtomicReference<ArmState>()

    var state: ArmState
        get() = _currentState.get()
        private set(s) {
            _currentState.set(s)
        }

    override fun start() {
        // TODO do we need the interna bus?
//        joinTopic(REQUEST_TOPIC, KobotsSubscriber<KobotsAction> { handleRequest(it) })
    }

    // allow the home sequence to run as well if termination has been called
//    override fun canRun() = runFlag.get() || currentSequence.get() == homeSequence.name

    lateinit var stopLatch: CountDownLatch
    override fun stop() {
        stopLatch = CountDownLatch(1)
        // home the arm and wait for it
//        executeSequence(SequenceRequest(homeSequence))
        if (!stopLatch.await(30, TimeUnit.SECONDS)) {
            logger.error("Arm not homed in 30 seconds")
        }
        super.stop()
    }

    override fun preExecution() {
        state = ArmState(state.position, true)
        HAStuff.noodSwitch.handleCommand("ON")
        DieAufseherin.currentMode = DieAufseherin.SystemMode.IN_MOTION
    }

    override fun postExecution() {
        with(HAStuff) {
            noodSwitch.handleCommand("OFF")
        }
        // just in case, release steppers
        DieAufseherin.currentMode = DieAufseherin.SystemMode.IDLE
        if (::stopLatch.isInitialized) stopLatch.countDown()
    }

}
