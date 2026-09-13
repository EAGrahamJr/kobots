/*
 * Copyright 2022-2026 by E. A. Graham, Jr.
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

package crackers.kobots.app.mechanicals

import com.diozero.api.ServoTrim
import com.diozero.devices.PCA9685
import com.diozero.devices.ServoController
import crackers.kobots.app.AppCommon
import crackers.kobots.app.HAJunk
import crackers.kobots.app.SystemState
import crackers.kobots.app.systemState
import crackers.kobots.parts.movement.async.AppScope
import crackers.kobots.parts.movement.async.AsyncRotator
import crackers.kobots.parts.movement.async.AsyncServoRotator
import crackers.kobots.parts.movement.async.SceneBuilder
import crackers.kobots.parts.movement.async.sceneBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * All the things
 */
object SuzerainOfServos : AppCommon.Startable {
    private val hat by lazy {
        ServoController(PCA9685())
    }

    private val stopLatch = AtomicBoolean(false)

    private val maxServoRange = 0..ServoTrim.MG90S.maxAngle
    lateinit var servos: List<AsyncRotator>
    lateinit var home: SceneBuilder

    private fun makeRotator(servoIndex: Int): AsyncRotator {
        val s = hat.getServo(servoIndex, ServoTrim.MG90S, 0)
        return object : AsyncServoRotator(s, maxServoRange) {
            override suspend fun myLittleKillSwitch(): Boolean = stopLatch.get()
        }
    }


    override fun start() {
        servos = (0..6).map { makeRotator(it) }.toList()

        home = sceneBuilder {
            servos.forEachIndexed { index, servo ->
                servo withSoftLaunch {
                    startDelay = (index * 500).milliseconds
                    angle = 0
                    duration = 2.seconds
                }
            }
        }
    }

    private val runLatch = Mutex()

    fun executeAct(act: suspend () -> Unit) {
        AppScope.appScope.launch {
            runLatch.withLock {
                if (!stopLatch.get()) {
                    preExecution()
                    act()
                    postExecution()
                }
            }
        }
    }


    override fun stop() {
        stopLatch.set(true)
    }

    fun preExecution() {
        systemState = SystemState.MOVING
    }

    fun postExecution() {
        HAJunk.sendUpdatedStates()
        systemState = SystemState.IDLE
    }

}
