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

package device.examples.general

import com.diozero.api.DigitalInputDevice
import com.diozero.devices.sandpit.motor.BasicStepperMotor
import com.diozero.devices.sandpit.motor.StepperMotorInterface
import com.diozero.sbc.LocalSystemInfo
import com.diozero.util.SleepUtil
import crackers.kobots.devices.display.SSD1327
import crackers.kobots.devices.expander.CRICKITHat
import crackers.kobots.utilities.PURPLE
import crackers.kobots.utilities.PointerGauge
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

/**
 * Start putting more stuff together in a "rational" format?
 */
class MovingAndDisplaying {
    private var running = true
    private fun checkRunning(stopIt: DigitalInputDevice) = running.let {
        if (running && stopIt.value) running = false
        running
    }

    private val hat by lazy {
        CRICKITHat()
    }

    private val neo by lazy {
        hat.neoPixel(39).apply {
            brightness = 0.05f
        }
    }

    private lateinit var gauge: PointerGauge
    private lateinit var image: BufferedImage
    private val oled by lazy {
        SSD1327(SSD1327.ADAFRUIT_STEMMA).apply {
            displayOn = false
            image = BufferedImage(128, 128, BufferedImage.TYPE_BYTE_GRAY).apply {
                gauge = PointerGauge(
                    this,
                    label = "Temp",
                    fontColor = Color.GREEN,
                    font = Font.SANS_SERIF,
                    minimumValue = 40.0,
                    maximumValue = 70.0
                )
            }
        }
    }

    fun SSD1327.showTemp(temp: Double) {
        gauge.setValue(temp)
        gauge.paint()
        display(image)
        show()
    }

    val stepper by lazy {
        BasicStepperMotor(hat.motorStepperPort())
    }

    val moveIt by lazy {
        hat.touchDigitalIn(2)
    }

    // simple case - run until the signal is given again (simulates another input)
    fun stepperRun() {
        oled.apply {
            displayOn = true
            image.graphics.apply {
                val previous = color
                color = Color.WHITE
                font = Font(Font.SANS_SERIF, Font.BOLD, 14)
                val fm = fontMetrics
                drawString("Starting stepper", 0, fm.ascent)
                color = Color.BLACK
                fillRect(0, 0, width, fm.height)
                color = previous
            }
            display(image)
            show()
            SleepUtil.sleepSeconds(2)
            displayOn = false
        }

        while (!moveIt.value) {
            stepper.step(StepperMotorInterface.Direction.FORWARD)
            SleepUtil.busySleep(Duration.ofMillis(10).toNanos())
        }
    }

    fun execute() {
        var cpuEndsAt: Instant = Instant.EPOCH

        var fill = PURPLE

        hat.use { hat ->
            neo.fill(fill)

            val displayIt = hat.touchDigitalIn(1)
            val stopIt = hat.touchDigitalIn(4)

            while (checkRunning(stopIt)) {
                val now = Instant.now()

                // manage color vs hour of day -------------------------------------------------------------
                LocalTime.now().hour.also { hour ->
                    when {
                        hour >= 21 -> if (fill == PURPLE) {
                            fill = Color.RED
                            neo.fill(fill)
                        }

                        hour > 8 && hour < 21 -> if (fill == Color.RED) {
                            fill = PURPLE
                            neo.fill(fill)
                        }
                    }
                }

                // show CPU temp on button press for 60 seconds -------------------------------------------
                if (cpuEndsAt.isBefore(now)) {
                    // turn it on
                    if (displayIt.value) {
                        oled.displayOn = true
                        cpuEndsAt = now.plusSeconds(60)
                    }
                    // if it's on, turn it off
                    else if (oled.displayOn) {
                        oled.displayOn = false
                    }
                }
                if (cpuEndsAt.isAfter(now)) {
                    val d = LocalSystemInfo.getInstance().cpuTemperature.toDouble()
                    oled.showTemp(d)
                }

                if (moveIt.value) stepperRun()

                SleepUtil.sleepSeconds(1)
            }
            stepper.release()
        }
        oled.close()
    }
}
