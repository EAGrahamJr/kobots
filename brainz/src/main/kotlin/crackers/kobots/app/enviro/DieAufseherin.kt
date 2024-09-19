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

package crackers.kobots.app.enviro

// import crackers.kobots.app.display.DisplayDos
import crackers.kobots.app.AppCommon
import crackers.kobots.app.AppCommon.ignoreErrors
import crackers.kobots.app.AppCommon.whileRunning
import crackers.kobots.app.CannedSequences
import crackers.kobots.app.Jimmy
import crackers.kobots.parts.movement.ActionSequence
import crackers.kobots.parts.movement.SequenceRequest
import crackers.kobots.parts.scheduleAtFixedRate
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/*
 * Central control, of a sorts.
 */
object DieAufseherin : AppCommon.Startable {
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
//            logger.info("System mode changed to $v")
            theMode.set(v)
//            if (v == SystemMode.IDLE) resetThings()
        }

    enum class BrainzActions {
        HOME,
        SAY_HI,
        STOP,
        CLUCK,
        RANDOM_EYES,
        RESET,
        THERMO_RESET,
    }

    private val logger = LoggerFactory.getLogger("DieAufseherin")

    // rock 'n' roll ================================================================================================

    override fun start() {
        localStuff()
        mqttStuff()
        HAStuff.start()
    }

    override fun stop() {
//        noodSwitch.handleCommand("OFF")
//        rosetteStrand.handleCommand("OFF")
    }

    private fun localStuff() {
        AppCommon.hasskClient.run {
            val block = {
                val elevation = sensor("sun_solar_elevation").state().state.toFloat().roundToInt()
                val azimuth = sensor("sun_solar_azimuth").state().state.toFloat().roundToInt()
                logger.debug("elevation: $elevation, azimuth: $azimuth")
                CannedSequences.setSun(azimuth, elevation)?.let { seq -> jimmy(seq) }
            }
            AppCommon.executor.scheduleAtFixedRate(30.seconds, 5.minutes) {
                whileRunning { ignoreErrors(block, true) }
            }
        }
    }

    private fun mqttStuff() {
        with(AppCommon.mqttClient) {
            startAliveCheck()
        }
    }

    private fun jimmy(sequence: ActionSequence) {
        Jimmy.handleRequest(SequenceRequest(sequence))
    }

    internal fun actionTime(payload: BrainzActions?) {
        when (payload) {
            BrainzActions.STOP -> {
                currentMode = SystemMode.IDLE
                Jimmy.stop()
                AppCommon.applicationRunning = false
            }

            BrainzActions.HOME -> jimmy(CannedSequences.home)
            BrainzActions.RESET -> jimmy(CannedSequences.resetHome)
            BrainzActions.SAY_HI -> jimmy(CannedSequences.holySpit)
            BrainzActions.THERMO_RESET -> jimmy(VeryDumbThermometer.reset)
            else -> logger.warn("Unknown command: $payload")
        }
    }
}
