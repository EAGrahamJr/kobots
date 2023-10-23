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
import crackers.kobots.app.execution.ROTO_PICKUP
import crackers.kobots.app.execution.ROTO_RETURN
import crackers.kobots.devices.display.HT16K33
import crackers.kobots.devices.display.QwiicAlphanumericDisplay
import crackers.kobots.parts.app.joinTopic
import crackers.kobots.parts.movement.SequenceExecutor
import org.slf4j.LoggerFactory

/**
 * 4 digit 14 segment display
 */
object Segmenter : AutoCloseable {
    private val logger = LoggerFactory.getLogger("Segmenter")
    private val segmenter = QwiicAlphanumericDisplay().apply {
        brightness = 0.05f
        autoShow = false
    }

    fun start() {
        joinTopic(
            SequenceExecutor.INTERNAL_TOPIC,
            { msg: SequenceExecutor.SequenceEvent ->
                logger.debug("Sequence completed: {}", msg)
                when (msg.sequence) {
                    ROTO_PICKUP -> with(segmenter) {
                        print("DROP")
                        blinkRate = HT16K33.BlinkRate.SLOW
                        show()
                    }

                    ROTO_RETURN -> with(segmenter) {
                        fill(false)
                        blinkRate = HT16K33.BlinkRate.OFF
                        show()
                    }
                }
            }
        )
        joinTopic(SLEEP_TOPIC, { it: AppCommon.SleepEvent ->
            logger.debug("Sleeping")
            segmenter.blinkRate = HT16K33.BlinkRate.OFF
            segmenter.fill(false)
            segmenter.show()
        })
    }

    override fun close() = stop()
    fun stop() {
        segmenter.close()
    }
}
