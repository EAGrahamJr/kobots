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

package crackers.kobots.app.bus

import crackers.kobots.app.parts.*

// TODO define a "system wide" event bus to capture all events and actions

/**
 * Base parts for all movements.
 */
abstract class MovementBuilder(protected var actuator: Actuator<Movement>? = null) {
    var relative: Boolean = false
    var stopCheck: () -> Boolean = { false }

    fun build(): Pair<Actuator<Movement>, Movement> {
        return Pair(actuator!!, makeMovement())
    }

    protected abstract fun makeMovement(): Movement
}

class RotationMovementBuilder(rotator: Rotator) : MovementBuilder(rotator as Actuator<Movement>) {
    var angle: Float = 0f

    override fun makeMovement(): Movement {
        return RotationMovement(angle, relative, stopCheck)
    }
}

enum class ActionSpeed {
    VERY_SLOW, SLOW, NORMAL, FAST, VERY_FAST
}

/**
 * Builds up movements for an action.
 */
class ActionBuilder {
    private val steps = mutableListOf<MovementBuilder>()
    var requestedSpeed: ActionSpeed = ActionSpeed.NORMAL

    infix fun Rotator.rotate(init: RotationMovementBuilder.() -> Unit) {
        steps += RotationMovementBuilder(this).apply(init)
    }

    infix fun Rotator.forwardUntil(forwardCheck: () -> Boolean) {
        steps += RotationMovementBuilder(this).apply {
            angle = Float.MAX_VALUE
            stopCheck = forwardCheck
        }
    }

    infix fun Rotator.backwardUntil(backwardCheck: () -> Boolean) {
        steps += RotationMovementBuilder(this).apply {
            angle = -Float.MAX_VALUE
            stopCheck = backwardCheck
        }
    }

    fun build(): Pair<Action, ActionSpeed> {
        return Action(steps.map { it.build() }) to requestedSpeed
    }
}

/**
 * Associates an [Action] with a [speed]. Note that the `Action` is rebuilt each time this is called.
 */
class ExecutableAction internal constructor(private val builder: ActionBuilder) {
    val action: Action
        get() = builder.build().first
    val speed: ActionSpeed = builder.build().second
}

/**
 * Defines a sequence of actions to be performed.
 */
class ActionSequence {
    private val steps = mutableListOf<ActionBuilder>()

    /**
     * Add an action to the sequence.
     */
    fun action(init: ActionBuilder.() -> Unit): ActionBuilder = ActionBuilder().apply(init).also {
        steps += it
    }

    operator fun plus(ab: ActionBuilder) {
        steps += ab
    }

    /**
     * Build the sequence of actions.
     */
    fun build(): List<ExecutableAction> = steps.map { ExecutableAction(it) }
}

/**
 * Typesafe "builder" (DSL) for creating a sequence of actions.
 */
fun sequence(init: ActionSequence.() -> Unit): ActionSequence = ActionSequence().apply(init)

/**
 * Request to execute a sequence of actions.
 */
class SequenceRequest(val sequence: ActionSequence, override val interruptable: Boolean = true) : KobotsAction
