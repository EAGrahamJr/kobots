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

package crackers.kobots.app

import crackers.kobots.app.Jimmy.crickitNeoPixel
import crackers.kobots.app.Jimmy.sunAzimuth
import crackers.kobots.app.Jimmy.sunElevation
import crackers.kobots.app.Jimmy.wavyThing
import crackers.kobots.devices.lighting.WS2811
import crackers.kobots.parts.ORANGISH
import crackers.kobots.parts.app.KobotSleep
import crackers.kobots.parts.colorIntervalFromHSB
import crackers.kobots.parts.movement.ActionSequence
import crackers.kobots.parts.movement.ActionSpeed
import crackers.kobots.parts.movement.DefaultActionSpeed
import crackers.kobots.parts.movement.sequence
import org.slf4j.LoggerFactory
import java.awt.Color

/**
 * Ibid
 */
object CannedSequences {
    val goFast =
        object : ActionSpeed {
            override val millis = 0L
        }

    var lightsOn = false
    val home =
        sequence {
            name = "Home"

            action {
                execute {
                    if (!lightsOn && crickitNeoPixel[7].color != Color.RED) {
                        crickitNeoPixel + WS2811.PixelColor(ORANGISH, brightness = .1f)
                        lightsOn = true
                    }
                    lightsOn
                }
            }

//            this append VeryDumbThermometer.home

            action {
                sunElevation rotate 0
                sunAzimuth rotate 0
                wavyThing rotate 0
            }
            action {
                execute {
                    crickitNeoPixel + Color.BLACK
                    true
                }
            }
        }

    val resetHome =
        sequence {
            name = "Reset Sun Azimuth"
            action {
                sunElevation rotate 0
                sunAzimuth rotate 0
            }
            action {
                execute {
                    sunAzimuth.apply {
                        reset()
                        release()
                    }
                    true
                }
            }
        }

    val holySpit =
        sequence {
            name = "Holy Spit!"
            action {
                wavyThing rotate 90
                requestedSpeed = DefaultActionSpeed.VERY_FAST
                execute {
                    crickitNeoPixel[0, 7] =
                        colorIntervalFromHSB(0f, 359f, 8)
                            .map { WS2811.PixelColor(it, brightness = .02f) }
                    true
                }
            }
            action {
                execute {
                    KobotSleep.seconds(15)
                    true
                }
            }
            action {
                wavyThing rotate 0
                execute {
                    crickitNeoPixel + Color.BLACK
                    true
                }
            }
        }

    const val AZIMUTH_OFFSET = 15

    fun setSun(
        azimuth: Int,
        elevation: Int,
    ): ActionSequence =
        (azimuth + AZIMUTH_OFFSET).let { az ->
            if (elevation < 0 || az >= Jimmy.ABSOLUTE_AZIMUTH_LIMIT) {
                home
            } else {
                run {
                    LoggerFactory.getLogger("Orrery").warn("Setting $az, $elevation")
                    sequence {
                        name = "Set Sun"
                        action {
                            sunAzimuth rotate az
                            sunElevation rotate elevation
                        }
                    }
                }
            }
        }

    fun setWavy(target: Float) = sequence { action { wavyThing rotate target.toInt() } }
}
