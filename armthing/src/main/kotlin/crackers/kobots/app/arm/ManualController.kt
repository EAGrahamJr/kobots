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

package crackers.kobots.app.arm

import crackers.kobots.app.AppCommon
import crackers.kobots.app.AppCommon.whileRunning
import crackers.kobots.app.manualMode
import crackers.kobots.devices.io.GamepadQT
import crackers.kobots.parts.scheduleWithFixedDelay
import java.util.concurrent.Future
import kotlin.time.Duration.Companion.milliseconds

/**
 * TODO fill this in
 */
object ManualController {
    private val gamepad by lazy { GamepadQT() }
    private var gpZeroX: Float? = null
    private var gpZeroY: Float? = null
    private lateinit var joyRideTFuture: Future<*>
    private var wasSelected = false

    fun start() {
        joyRideTFuture = AppCommon.executor.scheduleWithFixedDelay(20.milliseconds, 20.milliseconds) {
            // if the start button is pressed and was **not** pressed last time (debounce)
            whileRunning {
                wasSelected = if (gamepad.startButton) {
                    if (!wasSelected) {
                        manualMode = !manualMode
                        println("Manual mode is now $manualMode")
                    }
                    true
                } else {
                    if (manualMode) joyRide()
                    false
                }
            }
        }
    }

    fun stop() {
        joyRideTFuture.cancel(true)
        gamepad.close()
    }

    /**
     * Run a thing with the Gamepad
     *
     * TODO enable sending "remote" commands via MQTT - aka a selectable target with moving things?
     */
    private fun joyRide() = try {
        with(TheArm) {
            val xAxis = gamepad.xAxis
            if (gpZeroX == null) gpZeroX = xAxis
            else {
                val diff = gpZeroX!! - xAxis
                if (diff > 45f) waist -= 2
                if (diff < -45f) waist += 2
            }
            val yAxis = gamepad.yAxis
            if (gpZeroY == null) gpZeroY = yAxis
            else {
                val diff = gpZeroY!! - yAxis
                if (diff > 45f) {
                    val current = elbow.current()
//                    println ("elbow $current")
                    elbow.rotateTo(current - 3)
                }
                if (diff < -45f) {
                    val current = elbow.current()
//                    println ("elbow $current")
                    elbow.rotateTo(current + 3)
                }
            }


            if (gamepad.aButton) +extender
            if (gamepad.yButton) -extender
            if (gamepad.xButton) -gripper
            if (gamepad.bButton) +gripper

            updateCurrentState()
        }
    } catch (_: IllegalArgumentException) {
        // ignore
    }
}
