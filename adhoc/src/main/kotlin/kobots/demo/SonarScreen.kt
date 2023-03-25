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

package kobots.demo

import com.diozero.devices.HCSR04
import com.diozero.util.SleepUtil
import crackers.kobots.devices.at
import crackers.kobots.devices.display.SSD1327
import device.examples.R1_BLACK
import device.examples.R1_WHITE
import device.examples.checkStop
import device.examples.hat
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.geom.Line2D
import java.awt.image.BufferedImage
import java.lang.Thread.sleep
import java.time.Duration

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

/**
 * Combine sonar range-finder, servo, and OLED output to simulate a "sonar" screen
 */
class SonarScreen {
    private val servo by lazy { hat.servo(4) }
    private val zeroButton by lazy { hat.touchDigitalIn(1) }
    private val sweepButton by lazy { hat.touchDigitalIn(2) }

    private val screen by lazy {
        SSD1327(SSD1327.ADAFRUIT_STEMMA).apply { displayOn = true }
    }

    private fun BufferedImage.clear() {
        graphics.let {
            it.color = Color.BLACK
            it.fillRect(0, 0, width, height)
        }
    }

    private val image = BufferedImage(128, 128, BufferedImage.TYPE_BYTE_GRAY).apply {
        clear()
    }

    private val sonar by lazy { HCSR04(R1_WHITE, R1_BLACK) }

    fun execute() {
        hat.use {
            var direction = 0

            // "zero" servo for reference
            var currentAngle = START_ANGLE
            servo at START_ANGLE

            while (checkStop()) {
                if (direction != 0) {
                    paintIt(currentAngle, sonar.distanceCm)
                    screen.apply {
                        if (!displayOn) displayOn = true
                        screen.display(image)
                        screen.show()
                    }
                }

                val nextAngle = when {
                    direction == 1 -> currentAngle + SERVO_INTERVAL
                    direction == -1 -> currentAngle - SERVO_INTERVAL
                    else -> {
                        if (zeroButton.value) {
                            screen.displayOn = false
                            image.clear()
                            START_ANGLE
                        } else {
                            currentAngle
                        }
                    }
                }
                if (nextAngle != currentAngle) {
                    if (nextAngle > END_ANGLE) {
                        direction = -1
                    } else if (nextAngle < START_ANGLE) {
                        direction = 1
                    } else {
                        servo at nextAngle
                        currentAngle = nextAngle
                    }
                }

                if (sweepButton.value) {
                    println("Sweep")
                    sleep(2000)
                    direction = if (direction == 0) 1 else 0
                }

                SleepUtil.busySleep(Duration.ofMillis(5).toNanos())
            }
            servo at 0f
        }
        screen.close()
        sonar.close()
    }

    val MIDPOINT = 64.0
    val sweepLine = Line2D.Double(0.0, MIDPOINT, MIDPOINT, MIDPOINT)

    private fun paintIt(currentAngle: Float, distanceCm: Float) = with(image.graphics as Graphics2D) {
//        println("Distance ${distanceCm} at $currentAngle")

        // figure out our angle, etc. and draw a line to the edge (erase previous data)
        color = Color.WHITE
        val at = AffineTransform.getRotateInstance(Math.toRadians(currentAngle.toDouble()), MIDPOINT, MIDPOINT)
        draw(at.createTransformedShape(sweepLine))

        if (distanceCm > 100) return

        // now scale it and rotate and draw a scaled line - this erases from the outside in because the line origin
        // is at "0"
        color = Color.BLACK
        val scale = 1.0 - (distanceCm / 100.0)
        at.scale(scale, 1.0)
        draw(at.createTransformedShape(sweepLine))
    }

    companion object {
        private const val SERVO_INTERVAL = 1
        private const val START_ANGLE = 45f
        private const val END_ANGLE = 135f
    }
}
