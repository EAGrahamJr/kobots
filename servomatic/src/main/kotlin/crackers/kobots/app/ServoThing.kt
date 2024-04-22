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
import crackers.kobots.app.AppCommon.mqttClient
import crackers.kobots.app.HAJunk.commandSelectEntity
import crackers.kobots.app.newarm.ArmMonitor
import crackers.kobots.parts.app.KobotSleep
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * Handles a bunch of different servos for various things. Everything should have an HA interface.
 */

val logger = LoggerFactory.getLogger("Servomatic")

enum class Mode {
    IDLE, STOP, CLUCK, TEXT, HOME, SAY_HI, CRA_CRAY
}

internal interface Startable {
    fun start()
    fun stop()
}

// because we might be doing something else?
enum class SystemState {
    IDLE, MOVING, SHUTDOWN
}

private val state = AtomicReference(SystemState.IDLE)
internal var systemState: SystemState
    get() = state.get()
    set(v) {
        if (v != state.get()) {
            logger.warn("State is $v")
            state.set(v)
            // TODO trigger things?
            commandSelectEntity.sendCurrentState()
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
    mqttClient.apply {
        startAliveCheck()
        allowEmergencyStop()
    }

    AppCommon.awaitTermination()
    KobotSleep.seconds(1)
    exitProcess(0)
}

fun stopEverything() {
    if (systemState == SystemState.SHUTDOWN) {
        logger.warn("Already stopped")
        return
    }
    ArmMonitor.stop()
    SuzerainOfServos.stop()
    Sensei.stop()
    HAJunk.stop()

    AppCommon.executor.shutdownNow()

    logger.warn("Servomatic exit")
    systemState = SystemState.SHUTDOWN
}
