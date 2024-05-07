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


import crackers.kobots.app.newarm.DumbFunc
import crackers.kobots.app.newarm.Position
import crackers.kobots.app.newarm.Predestination
import crackers.kobots.mqtt.homeassistant.KobotSelectEntity
import crackers.kobots.parts.enumValue
import crackers.kobots.parts.movement.ActionSequence
import crackers.kobots.parts.movement.SequenceRequest
import crackers.kobots.app.SuzerainOfServos as Suzi

/**
 * TODO fill this in
 */
object Commando : KobotSelectEntity.Companion.SelectHandler {
    enum class Command {
        IDLE, STOP, MANUAL, TEXT, HOME, SAY_HI, CRA_CRAY, FOUR_TWENTY, PICKUP_1, PICKUP_2, GUARD
    }

    override val options = Command.entries.map { it.name }.sorted()

    fun sendItHome() {
        suzi(
            Predestination.gripperOpen +
                Predestination.outOfTheWay +
                Predestination.homeSequence
        )
    }

    private fun suzi(sequence: ActionSequence) {
        Suzi.handleRequest(SequenceRequest(sequence))
    }

    override fun executeOption(select: String) {
        val selected = enumValue<Command>(select)
        if (systemState == SystemState.MANUAL && selected != Command.IDLE && selected != Command.STOP) return

        when (selected) {
            Command.IDLE -> {
//                ManualController.stop()
                if (systemState == SystemState.MANUAL) {
                    systemState = SystemState.IDLE
                    sendItHome()
                }
            }

            Command.MANUAL -> {
                systemState = SystemState.MANUAL
//                ManualController.start()
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
            Command.PICKUP_1 -> {
                val pickup = DumbFunc.ArmAction(Position.first)
                val dropoff = DumbFunc.ArmAction(Position.waitForDropOff)
                val dropCheck = { HAJunk.proxSensor.currentState }
                val sequence = DumbFunc.grabFrom(pickup, 80) +
                    DumbFunc.grabFrom(dropoff, 0, dropCheck) +
                    Predestination.outOfTheWay +
                    Predestination.homeSequence
                suzi(sequence)
            }

            Command.PICKUP_2 -> {
                val pickup = DumbFunc.ArmAction(Position.tire)
                val lift = DumbFunc.ArmAction(Position.tireLift)
                val mid = DumbFunc.ArmAction(Position.tireMid)
                val fin = DumbFunc.ArmAction(Position.tireEnd)

                val rotateSequence = crackers.kobots.parts.movement.sequence {
                    name = "Tire Demo Midway"
                    this append lift.toAction()
                    this append mid.toAction()
                }
                val sequence = DumbFunc.grabFrom(pickup, 80) +
                    rotateSequence +
                    DumbFunc.grabFrom(fin, Suzi.GRIPPER_OPEN) +
                    Predestination.outOfTheWay +
                    Predestination.homeSequence
                suzi(sequence)
            }

            else -> logger.warn("No clue what to do with $select")
        }
    }
}
