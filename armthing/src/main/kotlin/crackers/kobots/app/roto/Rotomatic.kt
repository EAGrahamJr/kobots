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

package crackers.kobots.app.roto

import com.diozero.devices.sandpit.motor.BasicStepperMotor
import crackers.kobots.app.SequenceExecutor
import crackers.kobots.app.bus.KobotsAction
import crackers.kobots.app.bus.KobotsSubscriber
import crackers.kobots.app.bus.joinTopic
import crackers.kobots.app.crickitHat
import crackers.kobots.parts.RotatorStepper

/**
 * TODO fill this in
 */
object Rotomatic : SequenceExecutor() {

    const val REQUEST_TOPIC = "Rotomatic.Request"
    const val STATE_TOPIC = "Rotomatic.State"

    val mainRoto by lazy {
        val stepper = BasicStepperMotor(2048, crickitHat.unipolarStepperPort())
        RotatorStepper(stepper)
    }

    fun start() {
        joinTopic(
            REQUEST_TOPIC,
            KobotsSubscriber<KobotsAction> {
                handleRequest(it)
            }
        )
    }

    fun close() {
        mainRoto.release()
    }

    override fun preExecution() {
//        TODO("Not yet implemented")
    }

    override fun postExecution() {
        mainRoto.release()
    }

    override fun updateCurrentState() {
//        TODO("Not yet implemented")
    }
}
