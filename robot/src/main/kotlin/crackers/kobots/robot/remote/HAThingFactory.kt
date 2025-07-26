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

package crackers.kobots.robot.remote

import crackers.kobots.mqtt.homeassistant.DeviceIdentifier
import crackers.kobots.mqtt.homeassistant.KobotNumberEntity
import crackers.kobots.mqtt.homeassistant.KobotNumberEntity.Companion.NumberHandler
import crackers.kobots.parts.movement.*
import kotlin.math.roundToInt

/**
 * Makes things that merge HA "stuff" with actionable components.
 *
 * [name] is used mostly for prefixing the entity names so that stuff coming from this factory is technically
 * groupded together.
 */
class HAThingFactory(
    val name: String,
    val deviceIdentifier: DeviceIdentifier,
    private val sequenceExecutor: SequenceExecutor,
) {
    /**
     * Binds an HA number slider to a rotatable element. The default speed is `SLOW` because these are mostly servos
     * and fast is not good.
     */
    fun rotateThing(
        thing: String,
        rotator: Rotator,
        min: Int,
        max: Int,
        iconName: String = "mdi:rotate-360",
        speed: ActionSpeed = DefaultActionSpeed.SLOW,
    ): KobotNumberEntity {
        val handler =
            object : NumberHandler {
                override fun currentState() = rotator.current.toFloat()

                override fun set(target: Float) {
                    sequenceExecutor does
                        sequence {
                            name = "HA Move $thing"
                            action {
                                requestedSpeed = speed
                                rotator rotate target.roundToInt()
                            }
                        }
                }
            }
        return object : KobotNumberEntity(
            handler,
            "${name}_$thing".lowercase(),
            "$name: $thing",
            deviceIdentifier,
            min = min,
            max = max,
            unitOfMeasurement = "deg",
        ) {
            override val icon = iconName
        }
    }

    /**
     * Binds an HA number slider to a linear element. The default speed is `SLOW` because these are mostly servos
     * and fast is not good.
     */
    fun linearThing(
        thing: String,
        actuator: LinearActuator,
        min: Int,
        max: Int,
        iconName: String = "mdi:hand-pointing-right",
        speed: ActionSpeed = DefaultActionSpeed.SLOW,
    ): KobotNumberEntity {
        val handler =
            object : NumberHandler {
                override fun currentState() = actuator.current.toFloat()

                override fun set(target: Float) {
                    sequenceExecutor does
                        sequence {
                            name = "HA Move $thing"
                            action {
                                requestedSpeed = speed
                                actuator goTo target.roundToInt()
                            }
                        }
                }
            }
        return object : KobotNumberEntity(
            handler,
            "${name}_$thing".lowercase(),
            "$name: $thing",
            deviceIdentifier,
            min = min,
            max = max,
            unitOfMeasurement = "pct",
        ) {
            override val icon = iconName
        }
    }
}
