/*
 * Copyright 2022-2026 by E. A. Graham, Jr.
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

import crackers.kobots.app.Jimmy.rotorH
import crackers.kobots.app.Jimmy.rotorV
import crackers.kobots.parts.movement.async.EventBus
import crackers.kobots.parts.movement.async.KobotsEvent
import crackers.kobots.parts.movement.async.sceneBuilder
import kotlinx.coroutines.runBlocking
import java.lang.Thread.sleep
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

const val RUN_STUFF = "RUN_STUFF"

/**
 * Ibid
 */
object CannedSequences {

    class MoveMessage(
        val runThis: () -> Unit,
    ) : KobotsEvent {
        override val name = RUN_STUFF
    }

    fun wave_420() {
        val upScene = sceneBuilder {
            rotorV smoothly {
                angle = 180
                duration = 4.seconds
            }
            rotorH withSoftLanding {
                startDelay = 500.milliseconds
                angle = 180
                duration = 2.5.seconds
            }
        }
        val downScene = sceneBuilder {
            rotorV smoothly {
                angle = 0
                duration = 4.seconds
            }
            rotorH withSoftLanding {
                angle = 0
                duration = 2.seconds
            }
        }
        runBlocking {
            EventBus.publish(MoveMessage {
                upScene.play()
                sleep(500.milliseconds.inWholeMilliseconds)
                downScene.play()
            })
        }
    }
}
