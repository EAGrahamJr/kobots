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

package device.examples

import com.diozero.api.RuntimeIOException
import com.diozero.util.SleepUtil
import crackers.kobots.devices.expander.CRICKITHatDeviceFactory
import crackers.kobots.ops.createEventBus
import crackers.kobots.ops.stopTheBus
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Uses touchpad 4 to flip the "running" flag.
 */
abstract class RunManagerForFlows : AutoCloseable {
    val logger = LoggerFactory.getLogger(this::class.java.simpleName)

    // manage application run state
    val crickit by lazy { CRICKITHatDeviceFactory() }

    val shutdownBus = createEventBus<Boolean>(name = "Shutdown")
    val running = AtomicBoolean(true).also { run ->
        val touchMe = crickit.touchDigitalIn(4)

        // putting "true" on this will flip the `running` flag to false
        shutdownBus.apply {
            // if the button is pressed, stop running
            registerPublisher(errorHandler = errorMessageHandler) {
                try {
                    touchMe.value
                } catch (e: RuntimeIOException) {
                    logger.error("Error detected (${e.localizedMessage})- shutdown initiated")
                    true
                }
            }
            registerConsumer {
                if (it) {
                    run.set(false)
                    close()
                }
            }
        }
    }

    override fun close() {
        stopTheBus()
        crickit.close()
    }

    fun waitForIt(duration: Duration = 10.milliseconds, doSomething: () -> Unit = {}) {
        while (running.get()) {
            doSomething()
            SleepUtil.sleepMillis(duration.inWholeMilliseconds)
        }
    }
}
