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

import crackers.kobots.devices.io.GamepadQT

/**
 * TODO fill this in
 */
object ManualController {
    private val gamepad by lazy { GamepadQT() }
    private var gpZeroX: Float = 0f
    private var gpZeroY: Float = 0f

    /**
     * Run a thing with the Gamepad
     *
     * TODO enable sending "remote" commands via MQTT - aka a selectable target with moving things
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
//            if (gamepad.startButton) _manualMode.set(false)

            updateCurrentState()
        }
    }

}
