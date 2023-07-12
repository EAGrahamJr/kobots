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
import crackers.kobots.app.arm.TheArm.waist
import crackers.kobots.app.arm.sayHi
import crackers.kobots.app.bus.EnviroHandler
import crackers.kobots.app.bus.SequenceRequest
import crackers.kobots.app.bus.publishToTopic
import crackers.kobots.app.roto.Rotomatic
import crackers.kobots.app.roto.Rotomatic.mainRoto
import crackers.kobots.app.roto.getDrops
import crackers.kobots.app.roto.storeDrops
import crackers.kobots.devices.io.GamepadQT
import crackers.kobots.devices.io.NeoKey
import crackers.kobots.parts.ActionSequence
import org.tinylog.Logger
import java.awt.Color
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

private val _menuIndex = AtomicInteger(0)
val currentMenuItem: Menu
    get() = Menu.values()[_menuIndex.get()]

private val _manualMode = AtomicBoolean(false)
val manualMode: Boolean
    get() = _manualMode.get()

private var isEyeDrops = false

enum class Menu(val label: String, val action: () -> Unit) {
    HOME("Home", { publishSequence(homeSequence) }),

    //    PICK("Pick Up Drops", {
//        publishSequence(pickAndMove)
//        _menuIndex.incrementAndGet()
//    }),
//    RETURN_DROPS("Return to Sender", { publishSequence(returnTheThing) }),
    GET_DROPS("Rotomatic: Eye Drops", {
        publishToTopic(Rotomatic.REQUEST_TOPIC, SequenceRequest(getDrops))
        isEyeDrops = true
        _menuIndex.incrementAndGet()
    }),
    STORE_DROPS("Rotomatic: Store Drops", {
        if (isEyeDrops) {
            publishToTopic(Rotomatic.REQUEST_TOPIC, SequenceRequest(storeDrops))
            isEyeDrops = false
        }
    }),
    SAY_HI("Say Hi", { publishSequence(sayHi) }),
    MANUAL("Manual", { _manualMode.set(true) })
}

private fun publishSequence(sequence: ActionSequence) {
    publishToTopic(TheArm.REQUEST_TOPIC, SequenceRequest(sequence))
}

private const val WAIT_LOOP = 10L

/**
 * Run this.
 */
fun main() {
//    System.setProperty(REMOTE_PI, BRAINZ)

    SensorSuite.start()
    ArmMonitor.start()

    crickitHat.use { hat ->
        TheArm.start()
        Rotomatic.start()
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
                Logger.error("Exception", e)
            }
        }
        runFlag.set(false)
        TheArm.stop()
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

private var whichStepper = waist

private fun joyRide() {
    // do not let this interrupt anything else
    with(TheArm) {
        if (state.busy) return

        val xAxis = gamepad.xAxis
        if (gpZeroX == 0f) gpZeroX = xAxis
        val yAxis = gamepad.yAxis
        if (gpZeroY == 0f) gpZeroY = yAxis

        val stepperDelta = if (whichStepper == waist) 1f else 5f

        if (gpZeroX - xAxis > 45f) whichStepper += stepperDelta
        if (gpZeroX - xAxis < -45f) whichStepper -= stepperDelta
        if (gpZeroY - yAxis > 45f) -elbow
        if (gpZeroY - yAxis < -45f) +elbow
        if (gamepad.aButton) +extender
        if (gamepad.yButton) -extender
        if (gamepad.xButton) -gripper
        if (gamepad.bButton) +gripper
        if (gamepad.selectButton) {
            whichStepper = if (whichStepper == waist) {
                Logger.warn("Switching to mainRoto", null)
                mainRoto
            } else {
                Logger.warn("Switching to waist", null)
                waist
            }
        }
        if (gamepad.startButton) _manualMode.set(false)

        updateCurrentState()
    }
}

object Keyboard {
    private val keyboard = NeoKey().apply {
        brightness = 0.1f
    }

    private val BUTTON_COLORS = listOf(Color.BLUE, Color.GREEN, Color.CYAN, Color.RED)

    private val NO_BUTTONS = listOf(false, false, false, false)
    private var lastButtonValues = NO_BUTTONS
    lateinit var currentButtons: List<Boolean>

    // because we're looking for "presses", only return values when a value transitions _to_ true
    fun buttonCheck(): Boolean {
        currentButtons = try {
            keyboard.read().let { read ->
                // "auto repeat" for manual mode
                if (read == lastButtonValues) {
                    BUTTON_COLORS.forEachIndexed { index, color -> keyboard.pixels[index] = color }
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
            Logger.error("Error reading keyboard", e)
            NO_BUTTONS
        }
        return currentButtons.isEmpty() || !currentButtons[3]
    }

    fun close() {
        keyboard.close()
    }
}
