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

/**
 * Basic interface for a generic movement. A [stopCheck] function may be supplied to terminate movement, which should
 * be checked _prior_ to any physical action.
 */
interface Movement {
    val stopCheck: () -> Boolean
}

/**
 * A thing to handle a [Movement].
 */
interface Actuator<M : Movement> {
    /**
     * Perform the [Movement] and return `true` if the movement was successful/completed.
     */
    fun move(movement: M): Boolean
}

/**
 * An [Action] is a sequence of [Movmement]s to perform. Each [Movement] is associated with a [Actuator] and is to be
 * performed in sequence. The [Action] is considered complete when all [Movement]s have been performed.
 *
 * `Actions` are not re-runnable, nor are they thread-safe.
 */
class Action(movements: List<Pair<Actuator<Movement>, Movement>>) {

    // make a copy of the [movements] list to avoid any external modification
    private val _movements = mutableListOf<Pair<Actuator<Movement>, Movement>>().apply {
        addAll(movements)
    }
    private val movementResults = BooleanArray(_movements.size) { false }

    /**
     * Executes all the movments in the [Action] and returns `false` if all movements were successful.
     */
    fun step(): Boolean {
        val allDone = movementResults.mapIndexed { index, b ->
            if (!b) {
                val (actuator, movement) = _movements[index]
                (movement.stopCheck() || actuator.move(movement))
                    .also { movementResults[index] = it }
            } else {
                b
            }
        }.all { it }

        return !allDone
    }
}

/**
 * Describes where to go as a rotational angle. A [stopCheck] function may also be supplied to terminate movement
 * **prior** to reaching the desired [angle]. If the [relative] flag is set, the [angle] is relative to the current
 * position. An `Actuator` may or may not be limited in its range of motion, so the [angle] should be tailored to fit.
 */
open class RotationMovement(
    val angle: Float,
    val relative: Boolean = false,
    override val stopCheck: () -> Boolean = { false }
) : Movement

/**
 * Describes where to go as a [percentage] of "in/out", where "in" is 0 and 100 is "out". A [stopCheck] function may
 * also be supplied to terminate movement **prior** to reaching the desired setting. If the [relative] flag is set, the
 * [percentage] is relative to the current position, but cannot exceed the limits of the actuator.
 */
open class LinearMovement(
    val percentage: Int,
    val relative: Boolean = false,
    override val stopCheck: () -> Boolean = { false }
) : Movement
