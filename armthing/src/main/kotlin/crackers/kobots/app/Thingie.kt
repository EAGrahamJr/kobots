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

import crackers.kobots.app.NeoKeyBar.currentButtons
import crackers.kobots.app.arm.ArmMonitor
import crackers.kobots.app.arm.TheArm
import crackers.kobots.app.arm.TheArm.homeAction
import crackers.kobots.app.enviro.DieAufseherin
import crackers.kobots.app.enviro.SensorSuite
import crackers.kobots.app.enviro.VeryDumbThermometer
import crackers.kobots.app.execution.excuseMe
import crackers.kobots.app.execution.sayHi
import crackers.kobots.devices.io.GamepadQT
import crackers.kobots.execution.*
import org.tinylog.Logger
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess
import crackers.kobots.app.arm.TheArm.request as armRequest

private val _menuIndex = AtomicInteger(0)
val currentMenuItem: Menu
    get() = Menu.values()[_menuIndex.get()]

private val _manualMode = AtomicBoolean(false)
val manualMode: Boolean
    get() = _manualMode.get()

enum class Menu(val label: String, val action: () -> Unit) {
    HOME("Home", { armRequest(homeSequence) }),

//    PICK("Pick Up Drops", {
//        armRequest(PickWithRotomatic.moveStandingObjectToTarget)
//        _menuIndex.incrementAndGet()
//    }),
//    RETURN_DROPS("Return to Sender", {
//        armRequest(PickWithRotomatic.standingPickupAndReturn)
//        _menuIndex.incrementAndGet()
//    }),

    SAY_HI("Say Hi", { armRequest(sayHi) }),
    EXCUSE_ME("Excuse Me", { armRequest(excuseMe) }),
    MANUAL("Manual", { _manualMode.set(true) }),
    ROTO_NEXT("Roto Next", { mqtt.publish("kobots/rotoMatic", "next") }),
    ROTO_PREV("Roto Previous", { mqtt.publish("kobots/rotoMatic", "prev") }),
    ROTO_DROPS("Select Drops", { rotoSelect(0) }),
    ROTO_Thin1("Select Thin 1", { rotoSelect(1) }),
    ROTO_Thin2("Select Thin 2", { rotoSelect(2) }),
    ROTO_Thin3("Select Thin 3", { rotoSelect(3) }),
    ROTO_Thin4("Select Thin 4", { rotoSelect(4) })
}

private val WAIT_LOOP = Duration.ofMillis(50)

/**
 * Run this.
 */
fun main() {
//    System.setProperty(REMOTE_PI, BRAINZ)

    // these do not require the CRICKIT
    SensorSuite.start()
    ArmMonitor.start()

    crickitHat.use {
        // run the fan
        val fanMotor = it.motor(1)
        fanMotor.value = 1.0f

        // start all the things that require the CRICKIT
        VeryDumbThermometer.start()
        TheArm.start()

        // start auto-triggered stuff
        joinTopic(
            SLEEP_TOPIC,
            KobotsSubscriber<SleepEvent> {
                NeoKeyBar.brightness = if (it.sleep) 0.01f else .1f
            }
        )
        DieAufseherin.setUpListeners()

        // main loop!!!!!
        while (NeoKeyBar.buttonCheck() && runFlag.get()) {
            try {
                executeWithMinTime(WAIT_LOOP) {
                    // figure out if we're doing anything
                    when {
                        manualMode -> joyRide()
                        gamepad.xButton -> publishToTopic(TheArm.REQUEST_TOPIC, allStop)
                        currentButtons.any { it } -> buttonPresses()
                    }
                }
            } catch (e: Exception) {
                Logger.error(e, "Exception")
            }
        }
        runFlag.set(false)
        TheArm.stop()
        VeryDumbThermometer.stop()
        fanMotor.value = 0.0f
    }
    SensorSuite.close()
    ArmMonitor.stop()
    executor.shutdownNow()
    NeoKeyBar.close()
    exitProcess(0)
}

private fun buttonPresses() {
    when {
        // previous menu item
        currentButtons[0] -> {
            val next = _menuIndex.decrementAndGet().let {
                if (it < 0) Menu.values().size - 1 else it
            }
            _menuIndex.set(next)
        }

        // next menu item
        currentButtons[2] -> {
            val next = _menuIndex.incrementAndGet() % Menu.values().size
            _menuIndex.set(next)
        }

        // do the thing
        currentButtons[1] -> currentMenuItem.action()

        else -> {
            // do nothing
        }
    }
}

val homeSequence = crackers.kobots.parts.sequence {
    this + homeAction
}

private val gamepad by lazy { GamepadQT() }
private var gpZeroX: Float = 0f
private var gpZeroY: Float = 0f

private fun joyRide() {
    // do not let this interrupt anything else
    with(TheArm) {
        if (state.busy) return

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
