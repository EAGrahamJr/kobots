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

import crackers.kobots.devices.io.GamepadQT
import crackers.kobots.parts.app.publishToTopic
import crackers.kobots.parts.movement.SequenceRequest
import crackers.kobots.parts.scheduleWithFixedDelay
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Button presses and things
 *
 * TODO the I2C port on the Sparkfun Servo hat seems to be busted.
 */

object PadPrincipal {
    const val BBOARD_TOPIC = "kobots/buttonboard"
    const val ARMTHING_TOPIC = "kobots/gripOMatic"
    private val logger = LoggerFactory.getLogger("PadPrincipal")

    val gamePad = GamepadQT()

    var xMiddle = 0f
    var yMiddle = 0f
    var midCalibrationRuns = 0

    fun start() {
        AppCommon.executor.scheduleWithFixedDelay(2.seconds, 20.milliseconds) {
            if (AppCommon.applicationRunning) readTheButtons()
        }
    }

    private fun readTheButtons() {
        try {
            with(gamePad) {
                val (xj, yj) = Pair(xAxis, yAxis)

                // figure out where the "middle" of the joystick is
                if (midCalibrationRuns < 5) {
                    xMiddle += xj
                    yMiddle += yj
                    midCalibrationRuns++
                } else if (midCalibrationRuns == 5) {
                    xMiddle = xMiddle / midCalibrationRuns
                    yMiddle = yMiddle / midCalibrationRuns
                    midCalibrationRuns++
                } else {
//                    thingieSwirlie += 2 * gimmeDelta(xj, xMiddle)
                }


                // read the buttons
                with(read()) {
                    when {
                        start -> AppCommon.applicationRunning = false
                        a -> AppCommon.mqttClient.publish(BBOARD_TOPIC, "STOP")
                        y -> AppCommon.mqttClient.publish(ARMTHING_TOPIC, "STOP")
                        b -> publishToTopic(SuzerainOfServos.SERVO_TOPIC, SequenceRequest(swirlyMax))
                        x -> publishToTopic(SuzerainOfServos.SERVO_TOPIC, SequenceRequest(swirlyHome))

                        else -> {
                            // do nothing
                        }
                    }

                }
            }
        } catch (e: Exception) {
            LoggerFactory.getLogger("PadPrincipal").error("Error reading gamepad", e)
        }
    }

    private fun gimmeDelta(pos: Float, middle: Float): Int = if (pos < middle) -1 else 1

    fun stop() {
        gamePad.close()
    }
}
