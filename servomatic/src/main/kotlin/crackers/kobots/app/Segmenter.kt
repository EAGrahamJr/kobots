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

package crackers.kobots.app

import crackers.kobots.app.AppCommon.whileRunning
import crackers.kobots.devices.display.HT16K33
import crackers.kobots.devices.display.QwiicAlphanumericDisplay
import crackers.kobots.parts.elapsed
import crackers.kobots.parts.scheduleWithFixedDelay
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalTime
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds

/**
 * 4 digit 14 segment display
 */
object Segmenter : AutoCloseable {
    private val logger = LoggerFactory.getLogger("Segmenter")
    private val segmenter = QwiicAlphanumericDisplay().apply {
        brightness = 0.05f
        clear()
    }
    private lateinit var cluckFuture: Future<*>

    private enum class Mode {
        IDLE, CLUCK, TEXT
    }

    private val mode = AtomicReference(Mode.IDLE)

    lateinit var timerStart: Instant
    private var currentMode: Mode
        get() = mode.get()
        set(m) {
            logger.debug("Mode changed to {}", m)
            mode.set(m)
            timerStart = Instant.now()
            segmenter.clear()
        }

    private val TEXT_EXPIRES = java.time.Duration.ofSeconds(30)
    private val CLUCK_EXPIRES = java.time.Duration.ofSeconds(5)
    private val TIME_EXPIRES = java.time.Duration.ofSeconds(30)

    fun start() {
        segmenter.clear()
        var colon = true

        cluckFuture = AppCommon.executor.scheduleWithFixedDelay(1.seconds, 1.seconds) {
            whileRunning {
                try {
                val now = LocalTime.now()
                when (currentMode) {
                    // flip to cluck mode every 5 minutes
                    Mode.IDLE -> if (now.minute % 5 == 0 && now.hour in (8..20)) {
                        currentMode = Mode.CLUCK
                        segmenter.print("Clck")
                    }

                    // if text mode, assume the display contains the requested text already
                    Mode.TEXT -> {
                        if (timerStart.elapsed() > TEXT_EXPIRES) currentMode = Mode.IDLE
                        // TODO initiate a scroll?
                    }

                    Mode.CLUCK -> {
                        val elapsed = timerStart.elapsed()

                        when {
                            elapsed > TIME_EXPIRES -> currentMode = Mode.IDLE
                            elapsed > CLUCK_EXPIRES -> {
                                segmenter.clock(now, colon)
                                colon = !colon
                            }
                        }
                    }
                }
                } catch (e: Throwable) {
                    logger.error(e.localizedMessage)
                }
            }
        }
    }

    fun text(s: String) {
        if (s.isBlank()) {
            currentMode = Mode.IDLE
        } else {
            currentMode = Mode.TEXT
            segmenter.print(s)
        }
    }

    private fun QwiicAlphanumericDisplay.clear() {
        fill(false)
        blinkRate = HT16K33.BlinkRate.OFF
    }

    override fun close() = stop()
    fun stop() {
        if (::cluckFuture.isInitialized) try {
            cluckFuture.cancel(true)
            segmenter.clear()
            segmenter.close()
        } catch (e: Exception) {
            logger.error(e.localizedMessage)
        }
    }
}
