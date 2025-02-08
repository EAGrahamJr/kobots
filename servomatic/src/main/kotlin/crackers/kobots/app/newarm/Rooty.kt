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
import crackers.kobots.app.HAJunk
import crackers.kobots.app.SystemState
import crackers.kobots.app.dostuff.I2CFactory
import crackers.kobots.app.dostuff.SuzerainOfServos.elbow
import crackers.kobots.app.dostuff.SuzerainOfServos.shoulder
import crackers.kobots.app.dostuff.SuzerainOfServos.waist
import crackers.kobots.app.systemState
import crackers.kobots.devices.io.GamepadQT
import crackers.kobots.devices.io.QwiicTwist
import crackers.kobots.devices.lighting.WS2811.PixelColor
import crackers.kobots.parts.GOLDENROD
import crackers.kobots.parts.movement.LimitedRotator
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
            if (systemState == SystemState.IDLE) systemState = SystemState.MANUAL
            else if (systemState == SystemState.MANUAL) {
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
                while (!waist.rotateTo(next)) 1.milliseconds.sleep()
                waist.release()
                encoder.count = 0
            }
            // gamepad stuff: buttons are start, select, a, b, x, y
            gp.read().apply {
                if (a) {
                }
                if (b) {
                    // finger close
                }
                if (x) {
                }
                if (y) {
                }
                if (start) {
                }
                if (select) {
                }
            }

            // and also check the joystick
            (gp.xAxis - midX).let { diff ->
                // the elbow has to be "up-ish" to be able to move the shoulder
                if (elbow.current.toInt() > 30) {
                    if (diff > 100) shoulder.less() else if (diff < -100) shoulder.more()
                }
            }
            (gp.yAxis - midY).let { diff ->
                if (diff > 100) elbow.less() else if (diff < -100) elbow.more()
            }
        }
    }

    private fun LimitedRotator.more() = try {
        val next = current.toInt() + 1
        while (rotateTo(next)) 1.milliseconds.sleep()
    } catch (_: Throwable) {
    }

    private fun LimitedRotator.less() = try {
        val next = current.toInt() - 1
        while (rotateTo(next)) 1.milliseconds.sleep()
    } catch (_: Throwable) {
    }

    private lateinit var future: Future<*>

    override fun start() {
        encoder = QwiicTwist(I2CFactory.twistDevice).apply {
            pixel.brightness = .1f
            pixel.fill(Color.YELLOW)
            clearInterrupts() // clear buffers
        }
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

        future = AppCommon.executor.scheduleAtRate(30.milliseconds, ::clickOrTwist)
    }

    override fun stop() {
        if (::future.isInitialized) future.cancel(true)
        if (::encoder.isInitialized) encoder.pixel.off()
    }
}
