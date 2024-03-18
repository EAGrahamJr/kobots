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

import crackers.kobots.mqtt.homeassistant.DeviceIdentifier
import crackers.kobots.mqtt.homeassistant.KobotAnalogSensor
import crackers.kobots.mqtt.homeassistant.KobotSelectEntity
import crackers.kobots.mqtt.homeassistant.KobotTextEntity

/**
 * TODO fill this in
 */
object HAJunk : AutoCloseable {
    val haIdentifier = DeviceIdentifier("Kobots", "Servomatic")
    val textDosEntity = KobotTextEntity(Segmenter::text, "cluck_tower", "Cluck Tower", haIdentifier)

    val commandSelectEntity = KobotSelectEntity(selectHandler, "servo_selector", "Servo Selector", haIdentifier)

    val tofSensor = KobotAnalogSensor(
        "servomatic_tof", "Bobbi Detector", haIdentifier,
        KobotAnalogSensor.Companion.AnalogDevice.DISTANCE, unitOfMeasurement = "mm"
    )

    fun start() {
        Segmenter.start()
        textDosEntity.start()
        commandSelectEntity.start()
        tofSensor.start()
    }

    override fun close() {
        Segmenter.stop()
    }
}
