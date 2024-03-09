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

import com.diozero.devices.ServoController
import crackers.kobots.app.AppCommon.REMOTE_PI
import crackers.kobots.app.AppCommon.mqttClient
import crackers.kobots.app.SuzerainOfServos.INTERNAL_TOPIC
import crackers.kobots.mqtt.homeassistant.KobotSelectEntity
import crackers.kobots.parts.app.KobotSleep
import crackers.kobots.parts.app.publishToTopic
import crackers.kobots.parts.enumValue
import crackers.kobots.parts.movement.ActionSequence
import crackers.kobots.parts.movement.SequenceRequest
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * Handles a bunch of different servos for various things. Everything should have an MQTT interface.
 */
lateinit var hat: ServoController

val logger = LoggerFactory.getLogger("Servomatic")

enum class Mode {
    IDLE, STOP, CLUCK, TEXT
}

fun main(args: Array<String>?) {
    // pass any arg and we'll use the remote pi
    // NOTE: this reqquires a diozero daemon running on the remote pi and the diozero remote jar in the classpath
    if (args?.isNotEmpty() == true) System.setProperty(REMOTE_PI, args[0])

    // add the shutdown hook
    Runtime.getRuntime().addShutdownHook(thread(start = false, block = ::stopEverything))



    HAJunk.start()
    mqttClient.apply {
        startAliveCheck()
        allowEmergencyStop()
    }

    AppCommon.awaitTermination()
    KobotSleep.seconds(1)
    stopEverything()
    exitProcess(0)
}

fun stopEverything() {
    HAJunk.close()
//    hat.close()

    logger.warn("Servomatic exit")
}

internal fun servoRequest(sequence: ActionSequence) = publishToTopic(INTERNAL_TOPIC, SequenceRequest(sequence))

internal val selectHandler = object : KobotSelectEntity.Companion.SelectHandler {
    override val options = Mode.entries.map { it.name }
    override fun executeOption(select: String) {
        when (enumValue<Mode>(select)) {
            Mode.IDLE -> {

            }

            Mode.STOP -> AppCommon.applicationRunning = false
            Mode.CLUCK -> {

            }

            else -> logger.warn("No clue what to do with $select")
        }

    }
}
