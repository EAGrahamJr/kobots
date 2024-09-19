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

package crackers.kobots.app

import crackers.kobots.app.AppCommon.REMOTE_PI
import crackers.kobots.app.AppCommon.ignoreErrors
import crackers.kobots.app.AppCommon.mqttClient
import crackers.kobots.app.dostuff.Jeep
import crackers.kobots.app.dostuff.Sensei
import crackers.kobots.app.dostuff.SuzerainOfServos
import crackers.kobots.app.newarm.ArmMonitor
import crackers.kobots.app.newarm.Rooty
import crackers.kobots.devices.set
import crackers.kobots.parts.off
import crackers.kobots.parts.sleep
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds

/**
 * Handles a bunch of different servos for various things. Everything should have an HA interface.
 */

val logger = LoggerFactory.getLogger("Servomatic")

// because we might be doing something else?
enum class SystemState {
    IDLE,
    MOVING,
    SHUTDOWN,
    MANUAL
}

private val state = AtomicReference(SystemState.IDLE)
internal var systemState: SystemState
    get() = state.get()
    set(v) {
        val current = state.get()
        if (v != current) {
            logger.debug("State change from '$current' to '$v'")
            state.set(v)
            // TODO trigger things?
        }
    }

fun main(args: Array<String>?) {
    // pass any arg and we'll use the remote pi
    // NOTE: this reqquires a diozero daemon running on the remote pi and the diozero remote jar in the classpath
    if (args?.isNotEmpty() == true) System.setProperty(REMOTE_PI, args[0])

    // add the shutdown hook
    Runtime.getRuntime().addShutdownHook(thread(start = false, block = ::stopEverything))

    Sensei.start()
    SuzerainOfServos.start()
    ArmMonitor.start()
    HAJunk.start()
    ignoreErrors(Rooty::start, true)
    mqttClient.startAliveCheck()

    repeat(3) {
        Jeep.noodleLamp set .5f
        250.milliseconds.sleep()
        Jeep.noodleLamp set off
    }
    AppCommon.awaitTermination()
    stopEverything()
    exitProcess(0)
}

fun stopEverything() {
    if (systemState == SystemState.SHUTDOWN) {
        logger.warn("Already stopped")
        return
    }
    HAJunk.stop()
    SuzerainOfServos.stop()
    systemState = SystemState.SHUTDOWN

    ArmMonitor.stop()
    Sensei.stop()
    ignoreErrors(Rooty::stop)

    AppCommon.executor.shutdownNow()
    logger.warn("Servomatic exit")
}
