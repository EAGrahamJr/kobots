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

package dork

import com.diozero.api.I2CDevice
import com.diozero.devices.oled.SsdOledCommunicationChannel
import com.diozero.internal.provider.builtin.DefaultDeviceFactory
import com.diozero.internal.provider.remote.grpc.GrpcClientDeviceFactory
import com.diozero.util.Diozero
import crackers.kobots.devices.DebouncedButton
import crackers.kobots.devices.display.SSD1327
import crackers.kobots.devices.lighting.DEFAULT_SHIM_ADDRESS
import crackers.kobots.devices.lighting.PimoroniLEDShim
import crackers.kobots.devices.sensors.VCNL4040
import crackers.kobots.utilities.PointerGauge
import crackers.kobots.utilities.colorInterval
import crackers.kobots.utilities.scale
import kobots.ops.createEventBus
import kobots.ops.registerCPUTempConsumer
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/**
 * Stupid and pointless, but fun nevertheless.
 */
class DiozeroRelay {
    private val running = AtomicBoolean(true)

    private val localFactory = DefaultDeviceFactory().apply {
        Diozero.initialiseShutdownHook()
        start()
    }

    private val sensor: VCNL4040 = run {
        val i2CDevice = I2CDevice.builder(VCNL4040.QWIIC_I2C_ADDRESS)
            .setDeviceFactory(localFactory)
            .setController(1)
            .build()
        VCNL4040(i2CDevice).apply { ambientLightEnabled = true }
    }
    private val oled: SSD1327 = run {
        val i2CDevice = I2CDevice.builder(SSD1327.QWIIC_I2C_ADDRESS)
            .setDeviceFactory(localFactory)
            .setController(1)
            .build()
        SSD1327(SsdOledCommunicationChannel.I2cCommunicationChannel(i2CDevice))
    }
    private val killButton: DebouncedButton
    private val shim: PimoroniLEDShim

    init {
        killButton = DebouncedButton(17, Duration.ofMillis(50), deviceFactory = localFactory).apply {
            whenPressed {
                println("BUTTON")
                running.set(false)
            }
        }

        shim = run {
            val i2cDevice = I2CDevice.builder(DEFAULT_SHIM_ADDRESS)
                .setDeviceFactory(
                    GrpcClientDeviceFactory("marvin.local").apply {
                        start()
                    }
                )
                .setController(1)
                .build()
            PimoroniLEDShim(i2cDevice)
        }

        val gauge: PointerGauge
        val image = BufferedImage(128, 128, BufferedImage.TYPE_BYTE_GRAY).apply {
            gauge = PointerGauge(
                graphics as Graphics2D,
                128,
                128,
                label = "LUM",
                fontColor = Color.GREEN,
                font = Font.SERIF
            )
        }

        createEventBus<Float>().also { bus ->
            bus.registerConsumer {
                gauge.setValue(it.toDouble())
                gauge.paint()
                oled.display(image)
                oled.show()
            }
            bus.registerPublisher { sensor.luminosity }
        }

        // here's the crazy part
        val cpuColors = colorInterval(Color.RED, Color.GREEN, 28).map { it.scale(5) }.reversed().apply {
            forEachIndexed { index, color -> shim.pixelColor(index, color) }
        }
        var lastTemp = -1
        registerCPUTempConsumer { temp ->
            val x = ((temp - 45) * 28 / 20).roundToInt()
            if (x != lastTemp) {
                if (lastTemp != -1) shim.pixelColor(lastTemp, cpuColors[lastTemp])
                shim.pixelColor(x, Color.WHITE)
                lastTemp = x
            }
        }
    }

    fun run() {
        while (running.get()) {
            Thread.sleep(1000)
        }
        println("Done")
        shim.sleep(true)
        shim.close()
        sensor.close()
        oled.close()
        killButton.close()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DiozeroRelay().run()
        }
    }
}
