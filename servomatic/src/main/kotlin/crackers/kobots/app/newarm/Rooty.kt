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

package crackers.kobots.app.newarm

import crackers.kobots.app.AppCommon
import crackers.kobots.app.AppCommon.ignoreErrors
import crackers.kobots.app.SystemState
import crackers.kobots.app.dostuff.SuzerainOfServos.elbow
import crackers.kobots.app.dostuff.SuzerainOfServos.fingers
import crackers.kobots.app.dostuff.SuzerainOfServos.shoulder
import crackers.kobots.app.dostuff.SuzerainOfServos.waist
import crackers.kobots.app.dostuff.SuzerainOfServos.wrist
import crackers.kobots.app.systemState
import crackers.kobots.devices.io.QwiicTwist
import crackers.kobots.devices.lighting.WS2811
import crackers.kobots.parts.GOLDENROD
import crackers.kobots.parts.PURPLE
import crackers.kobots.parts.movement.Actuator
import crackers.kobots.parts.movement.BasicStepperRotator
import crackers.kobots.parts.movement.LinearActuator
import crackers.kobots.parts.movement.Rotator
import crackers.kobots.parts.scheduleAtRate
import crackers.kobots.parts.sleep
import org.slf4j.LoggerFactory
import java.awt.Color
import java.util.concurrent.Future
import kotlin.time.Duration.Companion.milliseconds

/**
 * Rotary encoder.
 */
object Rooty : AppCommon.Startable {
    private lateinit var encoder: QwiicTwist
    private val logger = LoggerFactory.getLogger("Rooty")

    private var lastMode = SystemState.IDLE

    private val mapTwistColors = mapOf(
        waist to Color.GREEN,
        shoulder to GOLDENROD,
        elbow to PURPLE,
        wrist to Color.BLUE,
        fingers to Color.MAGENTA
    )
    private val listOfTwist = listOf(waist, shoulder, elbow, wrist, fingers)
    private var lastTwist: Int = -1

    private fun WS2811.setIt() {
        if (lastTwist >= 0)
            fill(mapTwistColors[listOfTwist[lastTwist]]!!)
        else
            fill(Color.YELLOW)
    }

    val whichActuatorSelected: Actuator<*>?
        get() = if (lastTwist == -1 || lastTwist == listOfTwist.size) null else listOfTwist[lastTwist]


    private fun clickOrTwist() {
        if (systemState != SystemState.MOVING && encoder.clicked) {
            encoder.count = 0
            lastTwist++
            // check for state transition
            if (lastTwist == 0) systemState = SystemState.MANUAL
            else if (lastTwist >= listOfTwist.size) {
                systemState = SystemState.IDLE
                lastTwist = -1
            } else encoder.pixel.setIt()
        }

        if (lastMode != systemState) {
            // check if we need to change what we're doing
            lastMode = systemState
            when (systemState) {
                SystemState.IDLE -> {
                    lastTwist = -1
                    encoder.pixel.setIt()
                }

                SystemState.MOVING -> encoder.pixel.fill(Color.CYAN)
                SystemState.SHUTDOWN -> encoder.pixel.fill(Color.RED)
                SystemState.MANUAL -> encoder.pixel.setIt()
            }
        } else if (systemState == SystemState.MANUAL) {
            if (encoder.moved) {
                val diff = encoder.count
                whichActuatorSelected?.let { act ->
                    val next = act.current().toInt() + diff
                    ignoreErrors(moveSelectedThing(act, next), true)
                }
                encoder.count = 0
            }
        }
    }

    private fun moveSelectedThing(act: Actuator<*>, next: Int) = {
        if (act is Rotator) {
            while (!act.rotateTo(next)) 1.milliseconds.sleep()
            if (act is BasicStepperRotator) act.release()
        } else if (act is LinearActuator)
            while (!act.extendTo(next)) 1.milliseconds.sleep()
    }

    private lateinit var future: Future<*>

    override fun start() {
        encoder =
            QwiicTwist().apply {
                pixel.brightness = .1f
                pixel.fill(Color.YELLOW)
                clearInterrupts() // clear buffers
            }
        future = AppCommon.executor.scheduleAtRate(30.milliseconds, ::clickOrTwist)
    }

    override fun stop() {
        if (::future.isInitialized) future.cancel(true)
        if (::encoder.isInitialized) encoder.pixel.off()
    }
}
