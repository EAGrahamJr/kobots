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

import crackers.kobots.app.AppCommon
import crackers.kobots.app.AppCommon.SLEEP_TOPIC
import crackers.kobots.app.AppCommon.whileRunning
import crackers.kobots.app.execution.*
import crackers.kobots.devices.display.HT16K33
import crackers.kobots.devices.display.QwiicAlphanumericDisplay
import crackers.kobots.parts.app.KobotSleep
import crackers.kobots.parts.app.joinTopic
import crackers.kobots.parts.movement.SequenceExecutor
import crackers.kobots.parts.scheduleWithFixedDelay
import org.slf4j.LoggerFactory
import java.time.LocalTime
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

/**
 * 4 digit 14 segment display
 */
object Segmenter : AutoCloseable {
    private val logger = LoggerFactory.getLogger("Segmenter")
    private val segmenter = QwiicAlphanumericDisplay().apply {
        brightness = 0.05f
    }
    private lateinit var cluckFuture: Future<*>
    private val busy = AtomicBoolean(false)

    fun start() {
        joinTopic(
            SequenceExecutor.INTERNAL_TOPIC,
            { msg: SequenceExecutor.SequenceEvent ->
                logger.debug("Sequence completed: {}", msg)
                with(segmenter) {
                    val started = msg.started

                    when (msg.sequence) {
                        ROTO_PICKUP -> {
                            output("DROP")
                            blinkRate = HT16K33.BlinkRate.MEDIUM
                        }

                        ROTO_RETURN -> clear()
                        homeSequence.name -> clear()

                        sayHi.name -> {
                            if (started) output("Dude") else clear()
                        }

                        armSleep.name -> {
                            if (started) {
                                output("ZZZZ")
                                blinkRate = HT16K33.BlinkRate.SLOW
                            } else clear()
                        }

                        excuseMe.name -> {
                            if (started) {
                                output("SRRY")
                                blinkRate = HT16K33.BlinkRate.FAST
                            } else clear()
                        }

                        else -> {
                            if (started) output(msg.sequence.substring(0, 4))
                            else clear()
                        }
                    }
                }
            }
        )
        joinTopic(SLEEP_TOPIC, { it: AppCommon.SleepEvent ->
            if (it.sleep) {
                // once this goes to sleep, one of the above things has to occur to re-enable the display
                logger.debug("Sleeping")
                busy.set(true)
                KobotSleep.seconds(60)
                segmenter.clear()
            }
        })

        cluckFuture = AppCommon.executor.scheduleWithFixedDelay(10.seconds, 10.seconds) {
            whileRunning {
                // every 5 minutes, display "Clck" for 15 seconds, followed by the time for 45 seconds
                val now = LocalTime.now()
                if (now.minute % 5 == 0) {
                    if (!busy.get()) {
                        with(segmenter) {
                            print("Clck")
                            KobotSleep.seconds(15)
                            var colon = true
                            for (i in 1..45) {
                                // only display if not busy (e.g. with something else)
                                if (!busy.get()) clock(now, colon)
                                colon = !colon
                                KobotSleep.seconds(1)
                            }
                            // sleep an extra 2 seconds then clear: this should be enough time for the next minute to start
                            KobotSleep.seconds(2)
                            if (!busy.get()) fill(false)
                        }
                    }
                }
            }
        }
    }

    fun QwiicAlphanumericDisplay.output(thing: String) {
        busy.set(true)
        print(thing)
    }

    private fun QwiicAlphanumericDisplay.clear() {
        busy.set(false)
        fill(false)
        blinkRate = HT16K33.BlinkRate.OFF
    }

    override fun close() = stop()
    fun stop() {
        segmenter.close()
    }
}
