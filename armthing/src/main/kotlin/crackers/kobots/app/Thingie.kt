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
import crackers.kobots.app.AppCommon.executor
import crackers.kobots.app.AppCommon.ignoreErrors
import crackers.kobots.app.arm.ManualController
import crackers.kobots.app.arm.TheArm
import crackers.kobots.app.display.ArmMonitor
import crackers.kobots.app.display.DisplayDos
import crackers.kobots.app.enviro.DieAufseherin
import crackers.kobots.devices.expander.CRICKITHat
import crackers.kobots.devices.expander.I2CMultiplexer
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.system.exitProcess

private val crickitDelegate = lazy { CRICKITHat() }
private val muxDelegate = lazy { I2CMultiplexer() }

// shared devices
internal val crickitHat by crickitDelegate
internal val multiplexor by muxDelegate

private val logger = LoggerFactory.getLogger("BRAINZ")

internal interface Startable {
    fun start()
    fun stop()
}

private val startables = listOf(TheArm, ArmMonitor, DisplayDos, ManualController, DieAufseherin)
private val stopFlag = AtomicBoolean(true)

/**
 * Run this.
 */
fun main(args: Array<String>? = null) {
    // pass any arg and we'll use the remote pi
    // NOTE: this requires a diozero daemon running on the remote pi and the diozero remote jar in the classpath
    if (args?.isNotEmpty() == true) System.setProperty(REMOTE_PI, args[0])

    // start all the things to be started
    startables.forEach { it.start() }

    // and then we wait and stop
    Runtime.getRuntime().addShutdownHook(thread(start = false) { stopAll() })
    AppCommon.awaitTermination()
    logger.warn("Exiting")

    executor.shutdownNow()
    stopAll()
    logger.warn("Shutdown")
    exitProcess(0)
}

private fun stopAll() = with(stopFlag) {
    if (get()) {
        startables.forEach { ignoreErrors(it::stop) }

        if (muxDelegate.isInitialized()) multiplexor.close()
        if (crickitDelegate.isInitialized()) crickitHat.close()
    }
    set(false)
}

fun <F> ignoreErrors(executionBlock: () -> F?): F? =
    try {
        println("Stopping a thing")
        executionBlock()
    } catch (t: Throwable) {
        logger.error("Error on shutdown", t)
        null
    }
