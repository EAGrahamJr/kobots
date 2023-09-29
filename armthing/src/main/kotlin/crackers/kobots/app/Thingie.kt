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

import crackers.kobots.app.AppCommon.executor
import crackers.kobots.app.arm.ArmMonitor
import crackers.kobots.app.arm.TheArm
import crackers.kobots.app.enviro.DieAufseherin
import crackers.kobots.app.enviro.RosetteStatus
import crackers.kobots.app.enviro.VeryDumbThermometer
import crackers.kobots.devices.expander.CRICKITHat
import crackers.kobots.devices.io.GamepadQT
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

// shared devices
internal val crickitHat by lazy { CRICKITHat() }

private val _manualMode = AtomicBoolean(false)
val manualMode: Boolean
    get() = _manualMode.get()
private val logger = LoggerFactory.getLogger("BRAINZ")

/**
 * Run this.
 */
fun main(args: Array<String>? = null) {
    // pass any arg and we'll use the remote pi
    // NOTE: this requires a diozero daemon running on the remote pi and the diozero remote jar in the classpath
    if (args?.isNotEmpty() == true) System.setProperty(REMOTE_PI, args[0])

    // these do not require the CRICKIT
    ArmMonitor.start()

    crickitHat.use {
        // start all the things that require the CRICKIT
        VeryDumbThermometer.start()
        TheArm.start()
        DieAufseherin.start()
        RosetteStatus.start()

        AppCommon.awaitTermination()
        logger.error("Exiting")

        // stop all the things using the crickit
        RosetteStatus.stop()
        TheArm.stop()
        VeryDumbThermometer.stop()
        DieAufseherin.stop()
    }
    ArmMonitor.stop()
    executor.shutdownNow()
    exitProcess(0)
}

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
        if (gamepad.startButton) _manualMode.set(false)

        updateCurrentState()
    }
}
