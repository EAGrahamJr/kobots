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

import base.REMOTE_PI
import com.diozero.api.ServoTrim
import crackers.kobots.devices.at
import crackers.kobots.devices.lighting.PimoroniLEDShim
import crackers.kobots.ops.createEventBus
import crackers.kobots.utilities.colorInterval
import device.examples.RunManagerForFlows
import device.examples.flowCPUTemp
import java.awt.Color
import java.lang.Integer.min
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds

/**
 * Shooting for lots of things going on I2C bus.
 */
class MultiDeviceMadness : RunManagerForFlows() {
    private val shim = PimoroniLEDShim()
    private val anAdjustableValue = AtomicLong(0)
    val servo by lazy { crickit.servo(1, ServoTrim.TOWERPRO_SG90) }
    val motor by lazy { crickit.motor(1) }
    val neoPixel by lazy {
        crickit.neoPixel(30).apply {
            brightness = .1f
        }
    }

    init {
        logger.info("Starting setup")
        shim.flowCPUTemp()

        createEventBus<Boolean>(name = "ButtonUp").apply {
            val button = crickit.touchDigitalIn(1)
            registerPublisher(errorHandler = errorMessageHandler) { button.value }
            registerConsumer {
                if (it) {
                    val v = anAdjustableValue.addAndGet(10)
                    if (v == 0L) {
                        motor.stop()
                    } else {
                        motor at v / 100f
                    }
                }
            }
        }

        createEventBus<Boolean>(name = "ButtonDown").apply {
            val button = crickit.touchDigitalIn(2)
            registerPublisher(errorHandler = errorMessageHandler) { button.value }
            registerConsumer {
                if (it) {
                    val v = anAdjustableValue.addAndGet(-10)
                    if (v == 0L) {
                        motor.stop()
                    } else {
                        motor at v / 100f
                    }
                }
            }
        }

        createEventBus<Boolean>(name = "Output debug").apply {
            val button = crickit.signalDigitalIn(8)
            registerPublisher(errorHandler = errorMessageHandler) { button.value }
            registerConsumer(errorHandler = errorMessageHandler) {
                if (it) println("Debug statement")
            }
        }

        createEventBus<Float>(name = "Servo").apply {
            val xAxis = crickit.signalAnalogIn(6)
            registerPublisher(errorHandler = errorMessageHandler) { xAxis.unscaledValue }
            val lastServoValue = AtomicReference<Float>()
            registerConsumer(errorHandler = errorMessageHandler) { xValue ->
                val adjusted = xValue.round2Places()
                if (adjusted != lastServoValue.get()) {
                    lastServoValue.set(adjusted)
                    servo at 180 * adjusted
                }
            }
        }

        createEventBus<Float>(name = "YAxis").apply {
            val yAxis = crickit.signalAnalogIn(7)

            val colors = colorInterval(Color.RED, Color.GREEN, 15).let {
                it.reversed() + it
            }
            colors.forEachIndexed { index, color -> neoPixel[index] = color }

            registerPublisher(errorHandler = errorMessageHandler) { yAxis.unscaledValue }

            val lastY = AtomicInteger()
            registerConsumer(errorHandler = errorMessageHandler) { yValue: Float ->
                val adjusted = min((yValue.round2Places() * 30).roundToInt(), 29)
                lastY.get().also {
                    if (it != adjusted) {
                        neoPixel[adjusted] = Color.WHITE
                        neoPixel[it] = colors[it]
                        lastY.set(adjusted)
                    }
                }
            }
        }
    }

    fun Float.round2Places() = (this * 100).roundToInt() / 100f

    override fun close() {
        neoPixel.fill(Color.BLACK)
        shim.close()
        servo at 90f
        motor.stop()
        super.close()
    }

    fun execute() {
        waitForIt(200.milliseconds)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty(REMOTE_PI, "marvin.local")

            MultiDeviceMadness().use {
                it.execute()
            }
            exitProcess(0)
        }
    }
}
