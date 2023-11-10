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

import crackers.kobots.devices.sensors.VCNL4040
import crackers.kobots.parts.app.KobotsEvent
import crackers.kobots.parts.app.KobotsSubscriber
import crackers.kobots.parts.app.joinTopic
import crackers.kobots.parts.app.publishToTopic
import crackers.kobots.parts.movement.SequenceExecutor
import crackers.kobots.parts.movement.SequenceRequest
import crackers.kobots.parts.scheduleWithFixedDelay
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val proxy by lazy {
    val lastTriggered = AtomicReference<Instant>()

    joinTopic(SequenceExecutor.INTERNAL_TOPIC, KobotsSubscriber<KobotsEvent> { event ->
        if (event is SequenceExecutor.SequenceEvent) {
            if (event.sequence == steveTurns.name && !event.started) lastTriggered.set(Instant.now())
        }
    })


    VCNL4040()
        .apply {
            ambientLightEnabled = true
            proximityEnabled = true
        }
        .also { sensor ->
            var tripped = false

            AppCommon.executor.scheduleWithFixedDelay(5.seconds, 20.milliseconds) {
                val tTime = lastTriggered.get()
                val now = Instant.now()

                sensor.proximity.let { d ->
                    if (d > 5 && !tripped) {
                        publishToTopic(SuzerainOfServos.INTERNAL_TOPIC, SequenceRequest(steveTurns))
                        tripped = true
                    } else if (tTime != null && now.isAfter(tTime.plusSeconds(5))) {
                        if (tripped) {
                            publishToTopic(SuzerainOfServos.INTERNAL_TOPIC, SequenceRequest(steveGoesHome))
                            lastTriggered.set(null)
                        }
                        tripped = false
                    }

                }
            }
        }
}
