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

package crackers.kobots.app.enviro

import crackers.kobots.app.crickitHat
import crackers.kobots.app.mqtt
import crackers.kobots.devices.lighting.WS2811
import crackers.kobots.mqtt.KobotsMQTT
import org.slf4j.LoggerFactory
import java.awt.Color
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Blinken lights.
 */
object RosetteStatus {
    private val pixelStatus by lazy { crickitHat.neoPixel(8).apply { brightness = .005f } }
    private val lastCheckIn = mutableMapOf<String, ZonedDateTime>()
    private val aliveCheckExecutor = Executors.newSingleThreadScheduledExecutor()
    private val hostList = listOf("brainz", "marvin", "useless", "zeke")
    private val logger = LoggerFactory.getLogger("RosetteStatus")
    internal val goToSleep = AtomicBoolean(false)

    internal fun stop() {
        pixelStatus.fill(Color.BLACK)
    }

    /**
     * Listens for `KOBOTS_ALIVE` messages and tracks the last time a message was received from each host.
     */
    internal fun manageAliveChecks() {
        // store everybody's last time
        mqtt.subscribe(KobotsMQTT.KOBOTS_ALIVE) { s: String -> lastCheckIn[s] = ZonedDateTime.now() }

        // check for dead kobots
        val runner = Runnable {
            val now = ZonedDateTime.now()
            lastCheckIn.forEach { (host, lastSeenAt) ->
                val lastGasp = Duration.between(lastSeenAt, now).seconds
                val pixelNumber = hostList.indexOf(host)
                if (pixelNumber < 0) logger.warn("Unknown host $host")
                else {
                    pixelStatus[pixelNumber] = when {
                        goToSleep.get() -> WS2811.PixelColor(Color.BLACK, brightness = 0.0f)
                        lastGasp < 60 -> WS2811.PixelColor(Color.GREEN, brightness = 0.005f)
                        lastGasp < 120 -> WS2811.PixelColor(Color.YELLOW, brightness = 0.01f)
                        else -> WS2811.PixelColor(Color.RED, brightness = 0.1f)
                    }
                }
            }
            hostList.forEachIndexed { i, host ->
                if (!lastCheckIn.containsKey(host)) pixelStatus[i] = Color.BLUE
            }
        }
        aliveCheckExecutor.scheduleAtFixedRate(runner, 15, 15, TimeUnit.SECONDS)
    }

}
