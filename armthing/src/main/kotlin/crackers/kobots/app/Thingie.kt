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

import crackers.kobots.app.Keyboard.currentButtons
import crackers.kobots.app.arm.ArmMonitor
import crackers.kobots.app.arm.TheArm
import crackers.kobots.app.arm.TheArm.homeAction
import crackers.kobots.app.bus.EnviroHandler
import crackers.kobots.app.execution.EyeDropDemo
import crackers.kobots.app.execution.excuseMe
import crackers.kobots.app.execution.sayHi
import crackers.kobots.devices.io.GamepadQT
import crackers.kobots.devices.io.NeoKey
import crackers.kobots.execution.KobotsSubscriber
import crackers.kobots.execution.executeWithMinTime
import crackers.kobots.execution.joinTopic
import org.tinylog.Logger
import java.awt.Color
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

    PICK("Pick Up Drops", {
        armRequest(EyeDropDemo.pickupItem())
        _menuIndex.incrementAndGet()
    }),
    RETURN_DROPS("Return to Sender", {
        armRequest(EyeDropDemo.returnItem())
        _menuIndex.incrementAndGet()
    }),

    SAY_HI("Say Hi", { armRequest(sayHi) }),
    EXCUSE_ME("Excuse Me", { armRequest(excuseMe) }),
    MANUAL("Manual", { _manualMode.set(true) })
}

private val WAIT_LOOP = Duration.ofMillis(50)

/**
 * Run this.
 */
fun main() {
//    System.setProperty(REMOTE_PI, BRAINZ)

    // these do not require the CRICKIT
//    SensorSuite.start()
    ArmMonitor.start()

    crickitHat.use {
        // run the fan
        val fanMotor = it.motor(1)
        fanMotor.value = 1.0f

        // start all the things that require the CRICKIT
        VeryDumbThermometer.start()
        TheArm.start()

        // start auto-triggered stuff
        EnviroHandler.startHandler()

        // main loop!!!!!
        while (Keyboard.buttonCheck()) {
            try {
                executeWithMinTime(WAIT_LOOP) {
                    // figure out if we're doing anything
                    if (manualMode) {
                        joyRide()
                    } else if (currentButtons.any { it }) {
                        // we are, so do it
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
    Keyboard.close()
    exitProcess(0)
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

object Keyboard {
    private val keyboard = NeoKey().apply {
        brightness = 0.05f
    }

    private val BUTTON_COLORS = listOf(Color.BLUE, Color.GREEN, Color.CYAN, Color.RED)

    private val NO_BUTTONS = listOf(false, false, false, false)
    private var lastButtonValues = NO_BUTTONS
    private lateinit var _currentButtons: List<Boolean>
    val currentButtons: List<Boolean>
        get() = _currentButtons

    init {
        joinTopic(
            SLEEP_TOPIC,
            KobotsSubscriber { event ->
                if (event is SleepEvent) keyboard.brightness = if (event.sleep) 0.01f else 0.05f
            }
        )
    }

    // because we're looking for "presses", only return values when a value transitions _to_ true
    fun buttonCheck(): Boolean {
        _currentButtons = try {
            keyboard.read().let { read ->
                // nothing changed, make sure buttons are the "right" color
                if (read == lastButtonValues) {
                    BUTTON_COLORS.forEachIndexed { index, color ->
                        if (keyboard.color(index).color != color) keyboard.pixels[index] = color
                    }
                    NO_BUTTONS
                } else {
                    lastButtonValues = read
                    read.also {
                        it.forEachIndexed { index, b ->
                            keyboard.pixels[index] = if (b) Color.YELLOW else BUTTON_COLORS[index]
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Logger.error(e, "Error reading keyboard")
            NO_BUTTONS
        }
        return currentButtons.isEmpty() || !currentButtons[3]
    }

    fun close() {
        keyboard.close()
    }
}
