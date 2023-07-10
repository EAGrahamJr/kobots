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

import kotlin.random.Random

/**
 * A generic rotoator that can be used for testing.
 */
open class MockRotator : Rotator {
    var angle: Float = 0f
    var stopCheckResult = false

    override fun rotateTo(angle: Float): Boolean {
        var doneYet = stopCheckResult || this.angle == angle
        if (doneYet) return true

        if (angle > this.angle) {
            this.angle += 1f
        } else if (angle < this.angle) this.angle -= 1f
        doneYet = this.angle == angle
        return doneYet
    }

    override fun current(): Float = angle
}

fun runAndGetCount(block: () -> Boolean): Int {
    var count = 0
    while (block()) count++
    return count
}

fun randomServoAngle() = Random.nextInt(180).toFloat()
