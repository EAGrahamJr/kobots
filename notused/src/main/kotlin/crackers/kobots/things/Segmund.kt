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

package crackers.kobots.things

import crackers.kobots.app.AppCommon
import crackers.kobots.devices.display.QwiicAlphanumericDisplay
import crackers.kobots.parts.elapsed
import crackers.kobots.parts.scheduleAtRate
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds

/**
 * TODO fill this in
 */
object Segmund : AppCommon.Startable {
    private val logger = LoggerFactory.getLogger("Segmund")
    private lateinit var display: QwiicAlphanumericDisplay

    private lateinit var future: Future<*>

    private enum class Mode {
        OFF,
        CLOCK,
    }

    private val mode = AtomicReference(Mode.OFF)
    private val CLOCK_ON = Duration.ofSeconds(15)
    private var clockTime = Instant.EPOCH

    override fun start() {

        display =
            QwiicAlphanumericDisplay(listOf(I2CFactory.segmundDevice)).apply {
                brightness = 0.06f
                autoShow = true
            }
        var colon = true
        val lastMode = AtomicReference(Mode.OFF)

        future =
            AppCommon.executor.scheduleAtRate(1.seconds) {
                AppCommon.whileRunning {
                    if (lastMode.get() != mode.get()) {
                        lastMode.set(mode.get())
                        logger.info("Mode change: {}", mode.get())
                    }
                    when (mode.get()) {
                        Mode.CLOCK ->
                            if (clockTime.elapsed() < CLOCK_ON) {
                                with(display) {
                                    if (!on) on = true
                                    clock(LocalTime.now(), colon)
                                    colon = !colon
                                }
                            } else {
                                mode.set(Mode.OFF)
                            }

                        Mode.OFF ->
                            if (display.on) {
                                display.on = false
                                display.fill(false)
                            }

                        else -> {}
                    }
                }
            }
    }

    fun showTime() {
        if (mode.compareAndSet(Mode.OFF, Mode.CLOCK)) clockTime = Instant.now()
    }

    override fun stop() {
        if (::future.isInitialized) future.cancel(true)
        if (::display.isInitialized) {
            display.apply {
                fill(false)
                on = false
            }
        }
    }
}
