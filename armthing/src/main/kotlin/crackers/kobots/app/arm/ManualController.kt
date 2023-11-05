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
import crackers.kobots.app.manualMode
import crackers.kobots.devices.io.GamepadQT
import crackers.kobots.parts.scheduleWithFixedDelay
import org.slf4j.LoggerFactory
import java.util.concurrent.Future
import kotlin.time.Duration.Companion.milliseconds

/**
 * TODO fill this in
 */
object ManualController {
    private val gamepad by lazy { GamepadQT() }
    private var gpZeroX: Float = 0f
    private var gpZeroY: Float = 0f
    private lateinit var joyRideTFuture: Future<*>
    private var wasSelected = false

    fun start() {
        joyRideTFuture = AppCommon.executor.scheduleWithFixedDelay(20.milliseconds, 20.milliseconds) {
            // if the start button is pressed and was **not** pressed last time (debounce)
            try {
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
            } catch (_e: IllegalArgumentException) {

            } catch (t: Throwable) {
                LoggerFactory.getLogger("ManualController").error("Error in ManualController", t)
            }
        }

    }

    fun stop() {
        gamepad.close()
    }

    /**
     * Run a thing with the Gamepad
     *
     * TODO enable sending "remote" commands via MQTT - aka a selectable target with moving things?
     */
    private fun joyRide() {
        with(TheArm) {
            val xAxis = gamepad.xAxis
            if (gpZeroX == 0f) gpZeroX = xAxis
            val yAxis = gamepad.yAxis
            if (gpZeroY == 0f) gpZeroY = yAxis

            if (gpZeroX - xAxis > 45f) waist += 2
            if (gpZeroX - xAxis < -45f) waist -= 2
            if (gpZeroY - yAxis > 45f) +elbow
            if (gpZeroY - yAxis < -45f) -elbow
            if (gamepad.aButton) +extender
            if (gamepad.yButton) -extender
            if (gamepad.xButton) -gripper
            if (gamepad.bButton) +gripper

            updateCurrentState()
        }
    }

}
