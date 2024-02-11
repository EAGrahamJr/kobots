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
import crackers.kobots.app.arm.ArmMonitor
import crackers.kobots.app.arm.ManualController
import crackers.kobots.app.enviro.DieAufseherin
import crackers.kobots.devices.expander.CRICKITHat
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

// shared devices
internal val crickitHat by lazy { CRICKITHat() }

private val logger = LoggerFactory.getLogger("BRAINZ")

/**
 * Run this.
 */
fun main(args: Array<String>? = null) {
    // pass any arg and we'll use the remote pi
    // NOTE: this requires a diozero daemon running on the remote pi and the diozero remote jar in the classpath
    if (args?.isNotEmpty() == true) System.setProperty(REMOTE_PI, args[0])

    // these do not require the CRICKIT
    ArmMonitor.start()
//    Segmenter.start()
    ManualController.start()

    crickitHat.use {
        // start all the things that require the CRICKIT
//        TheArm.start()
        DieAufseherin.start()
//        RosetteStatus.start()

        AppCommon.awaitTermination()
        logger.warn("Exiting")
        // always "home" the Arm
//        TheArm.request(homeSequence)

        // stop all the things using the crickit
//        RosetteStatus.stop()
//        TheArm.stop()
        DieAufseherin.stop()
    }
    ManualController.stop()
//    Segmenter.stop()
    ArmMonitor.stop()

    logger.warn("Waiting 5 seconds for all background processes to clear...")
    executor.shutdownNow()
    logger.warn("Shutdown")
    exitProcess(0)
}
