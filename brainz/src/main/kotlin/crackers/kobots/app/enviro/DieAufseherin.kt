/*
 * Copyright 2022-2026 by E. A. Graham, Jr.
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

package crackers.kobots.app.enviro

// import crackers.kobots.app.display.DisplayDos
import crackers.kobots.app.AppCommon
import crackers.kobots.app.CannedSequences
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

/*
 * Central control, of a sorts.
 */
object DieAufseherin : AppCommon.Startable {
    private val logger = LoggerFactory.getLogger("DieAufseherin")

    // system-wide "wat the hell is going on" stuff
    enum class SystemMode {
        IDLE,
        IN_MOTION,
        MANUAL,
        SHUTDOWN,
    }

    private val theMode = AtomicReference(SystemMode.IDLE)
    var currentMode: SystemMode
        get() = theMode.get()
        set(v) {
            theMode.set(v)
        }

    enum class BrainzActions {
        STOP,
        THAT_TIME,
    }


    // rock 'n' roll ================================================================================================


    override fun stop() {
        // TODO: anything?
    }

    override fun start() {
//        AppCommon.hasskClient.run {
//            val block = {
//                val elevation =
//                    sensor("sun_solar_elevation")
//                        .state()
//                        .state
//                        .toFloat()
//                        .roundToInt()
//                val azimuth =
//                    sensor("sun_solar_azimuth")
//                        .state()
//                        .state
//                        .toFloat()
//                        .roundToInt()
//                logger.debug("elevation: $elevation, azimuth: $azimuth")
//                CannedSequences.setSun(azimuth, elevation).let { seq -> jimmy(seq) }
//            }
//            AppCommon.executor.scheduleAtFixedRate(30.seconds, 5.minutes) {
//                whileRunning { ignoreErrors(block, true) }
//            }
//        }
    }

    internal fun actionTime(payload: BrainzActions?) {
        logger.info(payload.toString())
        when (payload) {
            BrainzActions.STOP -> {
                currentMode = SystemMode.IDLE
                exitProcess(0)
            }

            BrainzActions.THAT_TIME -> {
                CannedSequences.wave_420()
            }

            else -> logger.warn("Unknown command: $payload")
        }
    }
}
