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

import crackers.kobots.app.arm.ArmMonitor
import crackers.kobots.app.arm.TheArm
import crackers.kobots.app.enviro.DieAufseherin
import crackers.kobots.app.enviro.DieAufseherin.DA_TOPIC
import crackers.kobots.app.enviro.DieAufseherin.dropOffRequested
import crackers.kobots.app.enviro.DieAufseherin.returnRequested
import crackers.kobots.app.enviro.VeryDumbThermometer
import crackers.kobots.app.execution.*
import crackers.kobots.app.io.NeoKeyHandler
import crackers.kobots.devices.io.GamepadQT
import crackers.kobots.execution.*
import crackers.kobots.parts.SequenceExecutor
import org.slf4j.LoggerFactory
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
    SAY_HI("Say Hi", { armRequest(sayHi) }),
    EXCUSE_ME("Excuse Me", { armRequest(excuseMe) }),
    MANUAL("Manual", { _manualMode.set(true) }),
    ROTO_NEXT("Roto Next", { mqtt.publish("kobots/rotoMatic", "next") }),
    ROTO_PREV("Roto Previous", { mqtt.publish("kobots/rotoMatic", "prev") }),
    ROTO_DROPS("Select Drops", { publishToTopic(DA_TOPIC, dropOffRequested) }),
    RETURN_PICK("Return last pick", { publishToTopic(DA_TOPIC, returnRequested) })
}

private val WAIT_LOOP = Duration.ofMillis(50)
const val REMOTE_PI = "diozero.remote.hostname"
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
    val keyboard = NeoKeyHandler()

    crickitHat.use {

        // start all the things that require the CRICKIT
        VeryDumbThermometer.start()
        TheArm.start()

        // start auto-triggered stuff
        joinTopic(
            SLEEP_TOPIC,
            KobotsSubscriber<SleepEvent> { event ->
                keyboard.brightness = if (event.sleep) 0.01f else .1f
            }
        )
        joinTopic(
            SequenceExecutor.INTERNAL_TOPIC,
            KobotsSubscriber<SequenceExecutor.SequenceCompleted> { msg ->
                when (msg.sequence) {
                    ROTO_PICKUP -> _menuIndex.set(Menu.RETURN_PICK.ordinal)
                    ROTO_RETURN -> _menuIndex.set(Menu.ROTO_DROPS.ordinal)
                    else -> {
                        // do nothing
                    }
                }
            })
        DieAufseherin.setUpListeners()

        // main loop!!!!!
        while (runFlag.get()) {
            try {
                executeWithMinTime(WAIT_LOOP) {
                    val currentButtons = keyboard.read()
                    // figure out if we're doing anything
                    when {
                        manualMode -> joyRide()
                        gamepad.xButton -> publishToTopic(TheArm.REQUEST_TOPIC, allStop)
                        currentButtons.any { it } -> buttonPresses(currentButtons)
                    }
                }
            } catch (e: Exception) {
                Logger.error(e, "Exception")
            }
        }
        logger.error("Exiting")
        rotoKill()
        runFlag.set(false)
        TheArm.stop()
        VeryDumbThermometer.stop()
    }
    ArmMonitor.stop()
    executor.shutdownNow()
    keyboard.close()
    exitProcess(0)
}

private fun buttonPresses(currentButtons: List<Boolean>) {
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
            runFlag.set(false)
        }
    }
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
