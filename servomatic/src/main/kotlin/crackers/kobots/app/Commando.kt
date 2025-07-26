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

import crackers.kobots.app.newarm.Predestination
import crackers.kobots.mqtt.homeassistant.KobotSelectEntity
import crackers.kobots.parts.enumValue
import crackers.kobots.parts.movement.ActionSequence
import crackers.kobots.parts.movement.SequenceRequest
import crackers.kobots.app.mechanicals.SuzerainOfServos as Suzi

/**
 * TODO fill this in
 */
object Commando : KobotSelectEntity.Companion.SelectHandler {
    enum class Command {
        IDLE,
        STOP,
        HOME,
        SAY_HI,
        CRA_CRAY,
        FOUR_TWENTY,
        GUARD,
    }

    override val options = Command.entries.map { it.name }.sorted()

    fun sendItHome() = suzi(Predestination.homeSequence)

    private fun suzi(sequence: ActionSequence) = Suzi.handleRequest(SequenceRequest(sequence))

    override fun executeOption(select: String) {
        val selected = enumValue<Command>(select)

        when (selected) {
            Command.IDLE -> {
                // TODO maybe show something stupid on the monitors?
            }

            Command.STOP -> {
                Suzi.stop()
                AppCommon.applicationRunning = false
            }

            Command.HOME -> sendItHome()

            Command.SAY_HI -> suzi(Predestination.sayHi)
            Command.CRA_CRAY -> suzi(Predestination.craCraSequence())
            Command.FOUR_TWENTY -> suzi(Predestination.fourTwenty)
            Command.GUARD -> suzi(Predestination.attackMode)

            else -> logger.warn("No clue what to do with $select")
        }
    }
}
