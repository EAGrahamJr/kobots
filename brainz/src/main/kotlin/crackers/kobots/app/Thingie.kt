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
import crackers.kobots.app.AppCommon.convenientShutdownHook
import crackers.kobots.app.AppCommon.convenientStartupHook
import crackers.kobots.app.AppCommon.executor
import crackers.kobots.app.display.DisplayDos
import crackers.kobots.app.display.LargerMonitor
import crackers.kobots.app.enviro.DieAufseherin
import crackers.kobots.devices.expander.I2CMultiplexer
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.system.exitProcess

// shared devices

internal lateinit var multiplexor: I2CMultiplexer

private val logger = LoggerFactory.getLogger("BRAINZ")

private val startables = listOf(DisplayDos, Jimmy, DieAufseherin, LargerMonitor)
private val stoppables = listOf(LargerMonitor, DieAufseherin, Jimmy, DisplayDos)
private val stopFlag = AtomicBoolean(false)

/**
 * Run this.
 */
fun main(args: Array<String>? = null) {
    // pass any arg and we'll use the remote pi
    // NOTE: this requires a diozero daemon running on the remote pi and the diozero remote jar in the classpath
    if (args?.isNotEmpty() == true) System.setProperty(REMOTE_PI, args[0])

    multiplexor = I2CMultiplexer()
    // start all the things to be started
    startables.convenientStartupHook()

    // and then we wait and stop
    Runtime.getRuntime().addShutdownHook(thread(start = false, block = ::stopAll))
    AppCommon.awaitTermination()
    logger.warn("Exiting")

    stopAll()
    logger.warn("Shutdown")
    exitProcess(0)
}

private fun stopAll() {
    if (stopFlag.compareAndSet(false, true)) {
        stoppables.convenientShutdownHook(true)
        executor.shutdownNow()
        multiplexor.close()
    }
}
