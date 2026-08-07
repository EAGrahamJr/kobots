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
import crackers.kobots.app.CannedSequences.goHome
import org.slf4j.LoggerFactory
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

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
            logger.info("Current mode $currentMode")
        }

    enum class BrainzActions {
        STOP,
        THAT_TIME,
        STEP_IT,
        IN_THE_POOL,
    }


    // rock 'n' roll ================================================================================================


    override fun stop() {
        // TODO: anything?
    }

    private lateinit var orneryFuture: Future<*>
    override fun start() {
        AppCommon.hasskClient.run {
            var lastAzimuth = 0
            val block = suspend {
                val elevation =
                    sensor("sun_solar_elevation")
                        .state()
                        .state
                        .toFloat()
                        .roundToInt()
                        .coerceIn(0, 180)
                val azimuth =
                    sensor("sun_solar_azimuth")
                        .state()
                        .state
                        .toFloat()
                        .roundToInt()
                val corrected = if (azimuth > 320) 0 else (azimuth + 30).coerceIn(0, 320)
                if (corrected != lastAzimuth) {
//                    logger.info("elevation: $elevation, azimuth: $azimuth (corrected :$corrected)")
//                    val move = sceneBuilder {
//                        rotor4 smoothly {
//                            angle = elevation
//                            duration = 3.seconds
//                        }
//                        driveStepperRotator smoothly {
//                            angle = corrected
//                            duration = 3.seconds
//                        }
//                    }
//                    move()
                    lastAzimuth = corrected
                }
            }
//            orneryFuture = AppCommon.executor.scheduleAtFixedRate(30.seconds, 5.minutes) {
//                if (currentMode != SystemMode.SHUTDOWN)
//                    EventBus.publish(CannedSequences.MoveMessage(block))
//            }
        }
    }

    internal fun actionTime(payload: BrainzActions?) {
        logger.info(payload.toString())
        when (payload) {
            BrainzActions.STOP -> {
                if (::orneryFuture.isInitialized) {
                    orneryFuture.cancel(false)
                }
                currentMode = SystemMode.SHUTDOWN
                goHome()
                AppCommon.applicationRunning = false
            }

            BrainzActions.THAT_TIME -> CannedSequences.wave_420()
            BrainzActions.STEP_IT -> CannedSequences.frontThing()
            BrainzActions.IN_THE_POOL -> CannedSequences.everybodyAllAtOnce()

            else -> logger.warn("Unknown command: $payload")
        }
    }
}
