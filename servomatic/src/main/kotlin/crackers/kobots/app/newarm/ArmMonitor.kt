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

package crackers.kobots.app.newarm

import com.diozero.devices.oled.SH1106
import com.diozero.devices.oled.SsdOledCommunicationChannel
import crackers.kobots.app.AppCommon
import crackers.kobots.app.Startable
import crackers.kobots.app.SystemState
import crackers.kobots.app.dostuff.I2CFactory.armMonitorDevice
import crackers.kobots.app.dostuff.SuzerainOfServos.ELBOW_MAX
import crackers.kobots.app.dostuff.SuzerainOfServos.elbow
import crackers.kobots.app.dostuff.SuzerainOfServos.shoulder
import crackers.kobots.app.dostuff.SuzerainOfServos.waist
import crackers.kobots.app.dostuff.SuzerainOfServos.wrist
import crackers.kobots.app.systemState
import crackers.kobots.graphics.widgets.DirectionPointer
import crackers.kobots.graphics.widgets.VerticalPercentageIndicator
import crackers.kobots.parts.app.KobotSleep
import crackers.kobots.parts.movement.LimitedRotator
import crackers.kobots.parts.off
import crackers.kobots.parts.on
import crackers.kobots.parts.scheduleWithFixedDelay
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.util.concurrent.Future
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/**
 * Shows where the arm is.
 */
object ArmMonitor : Startable {
    private val SMALL_FONT = Font(Font.SANS_SERIF, Font.BOLD, 10)

    private val screen by lazy {
        val comm = SsdOledCommunicationChannel.I2cCommunicationChannel(armMonitorDevice)
        SH1106(comm).apply {
            setContrast(0x20)
            clear()
            display = false
        }
    }

    private lateinit var graphics: Graphics2D
    private lateinit var image: BufferedImage

    private lateinit var future: Future<*>

    override fun start() {
        image = BufferedImage(screen.width, screen.height, screen.nativeImageType).also {
            graphics = (it.graphics as Graphics2D).apply {
                background = Color.BLACK
                color = Color.WHITE
            }
        }
        val waistPointer = DirectionPointer(
            graphics,
            SMALL_FONT,
            32,
            clockWise = false,
            endAngle = 180
        ).apply {
            drawStatic()
        }

        val shoulderPointer = DirectionPointer(
            graphics,
            SMALL_FONT,
            32,
            y = 32,
            endAngle = ELBOW_MAX,
            clockWise = false
        ).apply {
            drawStatic()
        }

        val elbowPointer = VerticalPercentageIndicator(
            graphics,
            SMALL_FONT,
            screen.height,
            x = shoulderPointer.bounds.x + shoulderPointer.bounds.width + 3,
            label = "Arm"
        ).apply {
            drawStatic()
        }
        val wristPointer = VerticalPercentageIndicator(
            graphics,
            SMALL_FONT,
            screen.height,
            x = elbowPointer.bounds.x + elbowPointer.bounds.width + 3,
            label = "Bkt"
        ).apply {
            drawStatic()
        }

        var screenOn = false
        future = AppCommon.executor.scheduleWithFixedDelay(100.milliseconds, 100.milliseconds) {
            AppCommon.whileRunning {
                when (systemState) {
                    SystemState.IDLE -> {
                        if (screenOn) {
                            screen.clear()
                            screen.display = off
                            screenOn = false
                        }
                    }

                    SystemState.SHUTDOWN -> {
                        // ignore
                    }

                    else -> {
                        if (!screenOn) {
                            screen.display = on
                            screenOn = true
                        }
                        waistPointer.updateValue(waist.current())
                        shoulderPointer.updateValue(shoulder.current())
                        elbowPointer.updateValue(elbow.percentage())
                        wristPointer.updateValue(wrist.percentage())
                        screen.display(image)
                    }
                }
            }
        }
    }

    private fun LimitedRotator.percentage(): Int {
        val scope = physicalRange.endInclusive - physicalRange.first
        return (current() * 100f / scope).roundToInt().coerceIn(0, 100)
    }

    override fun stop() {
        if (ArmMonitor::future.isInitialized) {
            future.cancel(true)
            while (!future.isDone) KobotSleep.millis(20)
        }
        screen.close()
    }
}
