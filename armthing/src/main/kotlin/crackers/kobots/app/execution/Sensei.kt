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

package crackers.kobots.app.execution

import crackers.kobots.app.arm.ArmMonitor
import crackers.kobots.app.arm.TheArm.waist
import crackers.kobots.app.arm.TheArm.waistServo
import crackers.kobots.devices.at
import crackers.kobots.devices.sensors.VL6180X
import crackers.kobots.parts.app.KobotSleep
import crackers.kobots.parts.movement.sequence

internal val toffle by lazy { VL6180X() }

private fun VL6180X.distance(): Float = try {
    distanceCm
} catch (e: Exception) {
    0f
}

val simpleScan by lazy {
    sequence {
        name = "Simple Scan"
        this += homeSequence

        // sweep the waist to 90 degrees, one degree at a time, then back to 0 the same way
        action {
//            execute { runWithServo() }
            waist forwardUntil {
                KobotSleep.millis(5)
                println("Angle ${waistServo.angle} waist ${waist.current()}- ${toffle.distance()}")
                ArmMonitor.ping(waist.current(), toffle.distance())
                KobotSleep.millis(15)
                waist.current() == 90
            }
        }
        action {
            waist backwardUntil {
                KobotSleep.millis(5)
                println("Angle ${waistServo.angle} waist ${waist.current()}- ${toffle.distance()}")
                ArmMonitor.ping(waist.current(), toffle.distance())
                KobotSleep.millis(15)
                waist.current() == 0
            }
        }
        this += homeSequence
    }
}

private fun runWithServo(): Boolean {
    fun snapShot(i: Int) {
        waistServo at i
        KobotSleep.millis(5)
        println("Angle $i waist ${waist.current()}- ${toffle.distance()}")
        KobotSleep.millis(15)
    }

    for (i in 0..140 step 2) snapShot(i)
    for (i in 140 downTo 0 step 2) snapShot(i)
    return true
}
