/*
 * Copyright 2022-2025 by E. A. Graham, Jr.
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
import crackers.kobots.app.HAJunk
import crackers.kobots.app.SystemState
import crackers.kobots.app.mechanicals.I2CFactory
import crackers.kobots.app.mechanicals.SuzerainOfServos.ELBOW_HOME
import crackers.kobots.app.mechanicals.SuzerainOfServos.FINGERS_OPEN
import crackers.kobots.app.mechanicals.SuzerainOfServos.SHOULDER_HOME
import crackers.kobots.app.mechanicals.SuzerainOfServos.WAIST_HOME
import crackers.kobots.app.mechanicals.SuzerainOfServos.WRIST_COCKED
import crackers.kobots.app.mechanicals.SuzerainOfServos.elbow
import crackers.kobots.app.mechanicals.SuzerainOfServos.fingers
import crackers.kobots.app.mechanicals.SuzerainOfServos.shoulder
import crackers.kobots.app.mechanicals.SuzerainOfServos.waist
import crackers.kobots.app.mechanicals.SuzerainOfServos.wrist
import crackers.kobots.app.systemState
import crackers.kobots.devices.io.GamepadQT
import crackers.kobots.devices.io.QwiicTwist
import crackers.kobots.devices.lighting.WS2811.PixelColor
import crackers.kobots.parts.GOLDENROD
import crackers.kobots.parts.movement.LimitedRotator
import crackers.kobots.parts.movement.LinearActuator
import crackers.kobots.parts.scheduleAtRate
import crackers.kobots.parts.sleep
import org.slf4j.LoggerFactory
import java.awt.Color
import java.time.LocalTime
import java.util.concurrent.Future
import kotlin.time.Duration.Companion.milliseconds

/**
 * Rotary encoder. It's button press enables/disables manual mode.
 */
object Rooty : AppCommon.Startable {
    private lateinit var encoder: QwiicTwist
    private lateinit var gp: GamepadQT
    private val logger = LoggerFactory.getLogger("Rooty")

    private var lastMode: SystemState? = null

    private var midX = 0f
    private var midY = 0f

    private const val LIGHT_ON = .3f

    private fun clickOrTwist() {
        if (encoder.clicked) {
            if (systemState == SystemState.IDLE) {
                systemState = SystemState.MANUAL
            } else if (systemState == SystemState.MANUAL) {
                HAJunk.sendUpdatedStates()
                waist.release()
                systemState = SystemState.IDLE
            }
        }
        if (lastMode != systemState) {
            // check if we need to change what we're doing
            lastMode = systemState
            when (systemState) {
                SystemState.IDLE -> encoder.pixel.fill(PixelColor(GOLDENROD, brightness = LIGHT_ON))
                SystemState.MOVING -> encoder.pixel.fill(PixelColor(Color.CYAN, brightness = .5f))
                SystemState.SHUTDOWN -> encoder.pixel.fill(PixelColor(Color.RED, brightness = 1f))
                SystemState.MANUAL -> encoder.pixel.fill(PixelColor(Color.GREEN, brightness = LIGHT_ON))
            }
        }

        // if manual mode, Suzi can't do anything, so it's up to this loop to do stuff
        if (systemState == SystemState.MANUAL) {
            // TODO this is a lot of shite to be going on in a single thread?
            encoder.pixel.brightness = if (LocalTime.now().second % 2 == 0) 0f else LIGHT_ON // blink the encoder
            if (encoder.moved) {
                val diff = encoder.count
                val next = (waist.current + diff).coerceIn(0, 359)
                waistGo(next)
                encoder.count = 0
            }
            // gamepad stuff: buttons are start, select, a, b, x, y
            gp.read().apply {
                if (a) {
                    fingers.more()
                }
                if (b) {
                    wrist.more()
                }
                if (x) {
                    wrist.less()
                }
                if (y) {
                    fingers.less()
                }
                if (start) {
                    fingers.go(FINGERS_OPEN)
                    wrist.go(WRIST_COCKED)
                    shoulder.go(30)
                    elbow.go(ELBOW_HOME)
                    shoulder.go(SHOULDER_HOME)
                    waistGo(WAIST_HOME)
                }
                if (select) {
                    // quit this mode?
                }
            }

            // and also check the joystick
            (gp.xAxis - midX).let { diff ->
                // the elbow has to be "up-ish" to be able to move the shoulder
                if (elbow.current.toInt() > 30) {
                    if (diff > 100) {
                        shoulder.less()
                    } else if (diff < -100) {
                        shoulder.more()
                    }
                }
            }
            (gp.yAxis - midY).let { diff ->
                if (diff > 100) {
                    elbow.less()
                } else if (diff < -100) {
                    elbow.more()
                }
            }
        }
    }

    private fun waistGo(next: Int) {
        while (!waist.rotateTo(next)) 1.milliseconds.sleep()
        waist.release()
    }

    private fun LinearActuator.more() = go(current.toInt() + 1)

    private fun LinearActuator.less() = go(current.toInt() - 1)

    private fun LinearActuator.go(where: Int) =
        try {
            while (extendTo(where)) 1.milliseconds.sleep()
        } catch (_: Throwable) {
        }

    private fun LimitedRotator.more() = go(current.toInt() + 1)

    private fun LimitedRotator.less() = go(current.toInt() - 1)

    private fun LimitedRotator.go(where: Int) =
        try {
            while (rotateTo(where)) 1.milliseconds.sleep()
        } catch (_: Throwable) {
        }

    private lateinit var future: Future<*>

    override fun start() {
        encoder = QwiicTwist(I2CFactory.twistDevice)
        gp = GamepadQT((I2CFactory.gamepadDevice))
        logger.warn("Standby - calibrating gamepad")
        repeat(5) {
            midX += gp.xAxis
            1.milliseconds.sleep()
            midY += gp.yAxis
            1.milliseconds.sleep()
        }
        midX /= 5
        midY /= 5

        encoder.apply {
            ignoreErrors({
                             pixel.brightness = .1f
                             pixel.fill(Color.YELLOW)
                             clearInterrupts() // clear buffers
                         })
        }

        future = AppCommon.executor.scheduleAtRate(30.milliseconds, ::clickOrTwist)
    }

    override fun stop() {
        if (::future.isInitialized) future.cancel(true)
        if (::encoder.isInitialized) encoder.pixel.off()
    }
}
