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

package kobots.ops

import base.REMOTE_PI
import com.diozero.api.DigitalInputDevice
import com.diozero.api.ServoDevice
import com.diozero.util.SleepUtil
import crackers.kobots.devices.expander.CRICKITHatDeviceFactory
import crackers.kobots.ops.createEventBus
import crackers.kobots.ops.stopTheBus
import java.lang.Thread.sleep
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Uses the same "scheme" of no threads as the Python code
 */
class PyKobot {
    fun execute() {
        CRICKITHatDeviceFactory().use { factory ->
            val stopButton = factory.touchDigitalIn(4)
            val upButton = factory.touchDigitalIn(1)
            val downButton = factory.touchDigitalIn(2)
            val servo = factory.servo(1).apply { angle = 90f }

            println("Starting no-thread style - press stop button to stop")
            pythonStyle(stopButton, upButton, servo, downButton)
            println("Starting event style - press stop button to stop")
            eventStyle(stopButton, upButton, servo, downButton)
        }
    }

    /**
     * Performs a "long-running" process - sets the servo to 0, then steps to the desired angle in 5 degree increments
     */
    @Synchronized
    fun ServoDevice.resetToAngle(goto: Float) {
        angle = 0f
        for (i in 0 until goto.toInt() step 10) {
            SleepUtil.busySleep(Duration.ofMillis(20).toNanos())
            angle = i.toFloat()
        }
    }

    fun eventStyle(
        stopButton: DigitalInputDevice,
        upButton: DigitalInputDevice,
        servo: ServoDevice,
        downButton: DigitalInputDevice
    ) {
        val running = AtomicBoolean(true)
        val angle = AtomicReference(90f)

        createEventBus<Boolean>(name = "stop").apply {
            registerPublisher { if (running.get()) stopButton.value else false }
            registerConditionalConsumer(messageCondition = { it }) {
                running.set(false)
            }
        }
        createEventBus<Boolean>(capacity = 1, name = "Up").apply {
            registerPublisher { if (running.get()) upButton.value else false }
            registerConditionalConsumer(errorHandler = errorMessageHandler, { it && angle.get() < 180f }) {
                val a = angle.get() + 10f
                servo.resetToAngle(a)
                angle.set(a)
            }
        }
        createEventBus<Boolean>(capacity = 1, name = "Down").apply {
            registerPublisher { if (running.get()) downButton.value else false }
            registerConditionalConsumer(
                errorHandler = errorMessageHandler,
                { it && angle.get() > 0f && running.get() }
            ) {
                val a = angle.get() - 10f
                servo.resetToAngle(a)
                angle.set(a)
            }
        }

        while (running.get()) {
            SleepUtil.busySleep(Duration.ofMillis(100).toNanos())
        }
        stopTheBus()
    }

    private fun pythonStyle(
        stopButton: DigitalInputDevice,
        upButton: DigitalInputDevice,
        servo: ServoDevice,
        downButton: DigitalInputDevice
    ) {
        var angle = 90f
        while (!stopButton.value) {
            if (upButton.value && angle < 180f) {
                angle += 10
                servo.resetToAngle(angle)
            } else if (downButton.value && angle > 0f) {
                angle -= 10
                servo.resetToAngle(angle)
            }
            sleep(50)
        }
    }
}

fun main() {
    System.setProperty(REMOTE_PI, "marvin.local")
    PyKobot().execute()
}
