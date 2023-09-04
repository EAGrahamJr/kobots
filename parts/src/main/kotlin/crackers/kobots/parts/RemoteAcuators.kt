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

package crackers.kobots.parts

import crackers.kobots.mqtt.KobotsMQTT

private abstract class RemoteSender(
    private val mqttClient: KobotsMQTT, private val topic: String, private val
    device: String
) {

}

private abstract class RemoteReceiver {

}

/**
 * TODO fill this in
 */
class RemoteRotatorSender : Rotator {
    override fun rotateTo(angle: Int): Boolean {
        TODO()
    }

    override fun current(): Int {
        TODO()
    }
}

class RemoteRotatorReceiver : Rotator {
    override fun rotateTo(angle: Int): Boolean {
        TODO()
    }

    override fun current(): Int {
        TODO()
    }
}

class RemoteLineaerSender : LinearActuator {
    override fun extendTo(percentage: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun current(): Int {
        TODO()
    }
}

class RemoteLinearReceiver : LinearActuator {
    override fun extendTo(percentage: Int): Boolean {
        TODO()
    }

    override fun current(): Int {
        TODO()
    }
}
