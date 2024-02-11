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

package crackers.kobots.app.execution

import crackers.kobots.app.arm.TheArm
import crackers.kobots.app.enviro.VeryDumbThermometer
import crackers.kobots.parts.movement.ActionSequence
import crackers.kobots.parts.movement.Rotator
import crackers.kobots.parts.movement.sequence
import org.slf4j.LoggerFactory

/**
 * TODO Processes "bang commands" from MQTT messages.
 */
object BangCommands {
    private val logger = LoggerFactory.getLogger("BangCommands")

    val turnyThings = mapOf(
        "thermo" to VeryDumbThermometer.thermoStepper,
        "waist" to TheArm.waist,
        "elbow" to TheArm.elbow,
        "moe" to TheArm.slowMoe
    )

    fun figureThisOut(rawCommand: String) {
        val tokens = rawCommand.split(" ").map { it.trim().lowercase() }
        when (tokens[0]) {
            "rotate" -> {
                val whosTurning = whichRotator(tokens)
                val degrees = tokens[2].toInt()
                logger.info("$whosTurning rotateTo $degrees")
                executeRotation(whosTurning, degrees)
            }

            "rotateBy" -> {
                val whosTurning =
                    turnyThings[tokens[1]] ?: throw IllegalArgumentException("Unknown turny thing: ${tokens[1]}")
                val delta = tokens[2].toInt()
                val current = whosTurning.current()
                logger.info("$whosTurning rotateBy $delta from $current")
                executeRotation(whosTurning, current + delta)
            }

            "stop" -> {
                logger.info("stop")
                TheArm.stop()
            }
        }
    }

    private fun whichRotator(tokens: List<String>) =
        turnyThings[tokens[1]] ?: throw IllegalArgumentException("Unknown turny thing: ${tokens[1]}")

    private fun executeRotation(whosTurning: Rotator, degrees: Int) {
        val runThis = sequence {
            if (whosTurning == VeryDumbThermometer.thermoStepper) {
                thermoAction(degrees)
            } else {
                action {
                    whosTurning rotate degrees
                }
            }
        }
        TheArm.request(runThis)
    }

    private fun ActionSequence.thermoAction(degrees: Int) {
        action {
            execute {
//                VeryDumbThermometer.justGo(degrees)
                true
            }
        }
    }
}
