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
import crackers.kobots.app.AppCommon.whileRunning
import crackers.kobots.app.Startable
import crackers.kobots.app.enviro.DieAufseherin
import crackers.kobots.app.enviro.DieAufseherin.GripperActions
import crackers.kobots.app.enviro.DieAufseherin.SystemMode
import crackers.kobots.app.enviro.VeryDumbThermometer.thermoStepper
import crackers.kobots.app.multiplexor
import crackers.kobots.devices.io.GamepadQT
import crackers.kobots.parts.scheduleWithFixedDelay
import org.slf4j.LoggerFactory
import java.util.concurrent.Future
import kotlin.time.Duration.Companion.milliseconds

/**
 * TODO fill this in
 */
object ManualController : Startable {
    private lateinit var gamepad: GamepadQT
    private var gpZeroX: Float? = null
    private var gpZeroY: Float? = null
    private lateinit var joyRideTFuture: Future<*>
    private var wasPressed = false

    override fun start() {
        try {
            gamepad = GamepadQT(multiplexor.getI2CDevice(7, GamepadQT.DEFAULT_I2C_ADDRESS))
        } catch (t: Throwable) {
            LoggerFactory.getLogger("ManualController").error("Unable to init manual controller", t)
            return
        }

        joyRideTFuture = AppCommon.executor.scheduleWithFixedDelay(20.milliseconds, 20.milliseconds) {
            whileRunning {
                val manualMode = DieAufseherin.currentMode == SystemMode.MANUAL

                wasPressed = gamepad.startButton.also {
                    if (!it && wasPressed) {
                        val switchThis = if (manualMode) {
                            thermoStepper.release()
                            GripperActions.HOME
                        } else {
                            GripperActions.MANUAL
                        }
                        DieAufseherin.actionTime(switchThis)
                    } else if (!it && manualMode) {
                        joyRide()
                    }
                }
            }
        }
    }

    override fun stop() {
        if (::gamepad.isInitialized) {
            joyRideTFuture.cancel(true)
            gamepad.close()
        }
    }

    private var armSelected = false
    private var selectDebounce = false

    /**
     * Run a thing with the Gamepad
     */
    private fun joyRide() {
        // select button changes the "mode"
        selectDebounce = gamepad.selectButton.also {
            if (!it && selectDebounce) armSelected = !armSelected
        }
        if (selectDebounce) return

        if (armSelected) {
            // armThing()
        } else {
            otherThing()
        }
    }

    private fun otherThing() = with(gamepad) {
        if (xButton) {
//            +thermoStepper
        } else if (bButton) {
//            -thermoStepper
        } else if (aButton) thermoStepper.reset()
    }

    private fun armThing() {
        with(TheArm) {
            val xAxis = gamepad.xAxis
            if (gpZeroX == null) gpZeroX = xAxis
            else {
                val diff = gpZeroX!! - xAxis
//                if (diff > 45f) +boomLink
//                if (diff < -45f) -boomLink
            }
            val yAxis = gamepad.yAxis
            if (gpZeroY == null) {
                gpZeroY = yAxis
            } else {
                val diff = gpZeroY!! - yAxis
//                if (diff > 45f) +armLink
//                if (diff < -45f) -armLink
            }

//            if (gamepad.aButton) +swing
//            if (gamepad.yButton) -swing
//            if (gamepad.xButton) -gripper
//            if (gamepad.bButton) +gripper

            updateCurrentState()
        }
    }

    fun statuses(): Map<String, Any> {
        return if (armSelected) {
            TheArm.state.position.mapped()
        } else {
            mapOf("THM" to thermoStepper.current(), "HI MOM" to 0.0f)
        }
    }
}
