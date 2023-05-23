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

package crackers.kobots.app

/**
 * Servo and ultrasonic sensor.
 */
object Sonar {
    private const val MIN_ANGLE = 45
    private const val MAX_ANGLE = 135
    private const val STEP = 2

//    private val servo by lazy { crickitHat.servo(4).apply {
//        angle = MIN_ANGLE.toFloat()
//    } }
//
//    fun start() {
//        checkRun(10) { rotateSpline()}
//    }
//
//    private var goingForward:Boolean = true
//    private fun rotateSpline() = with(servo){
//        angle = if (goingForward) {
//            val nextAngle = angle + STEP
//            if (nextAngle > MAX_ANGLE) {
//                goingForward = false
//                MAX_ANGLE.toFloat()
//            } else
//                nextAngle
//        } else {
//            val nextAngle = angle - STEP
//            if (nextAngle < MIN_ANGLE) {
//                goingForward = true
//                MIN_ANGLE.toFloat()
//            } else
//                nextAngle
//        }
//    }
}
