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

package crackers.kobots.app

import com.diozero.api.ServoDevice
import com.diozero.api.ServoTrim
import com.diozero.devices.ServoController
import crackers.kobots.app.SensorSuite.PROXIMITY_TOPIC
import crackers.kobots.execution.KobotsSubscriber
import crackers.kobots.execution.joinTopic
import crackers.kobots.mqtt.KobotsMQTT
import crackers.kobots.parts.ServoLinearActuator
import crackers.kobots.parts.ServoRotator
import crackers.kobots.utilities.KobotSleep
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

/**
 * Handles a bunch of different servos for various things. Everything should have an MQTT interface.
 *
 * - RotoMatic uses a Rotator to select a thing. Manual selections are through something with buttons via MQTT
 * - LiftOMatic uses a LinearActuator to raise and lower a thing: it's either up or down.
 */

lateinit var rotoServo: ServoDevice
val rotoMatic by lazy { ServoRotator(rotoServo, (0..180), (0..180)) }

lateinit var liftServo: ServoDevice
val liftoMatic by lazy { ServoLinearActuator(liftServo, 0f, 45f) }

val logger = LoggerFactory.getLogger("Servomatic")
val doneLatch = CountDownLatch(1)

val mqttClient = KobotsMQTT("marvin", "tcp://192.168.1.4:1883").apply {
    startAliveCheck()
    subscribe(SERVO_TOPIC) {
        val payload = ServoMaticCommand.valueOf(it.uppercase())
        when (payload) {
            ServoMaticCommand.STOP -> {
                logger.error("Stopping")
                doneLatch.countDown()
            }

            ServoMaticCommand.UP -> liftoMatic.move(100)
            ServoMaticCommand.DOWN -> liftoMatic.move(0)
            ServoMaticCommand.LEFT -> rotoMatic.swing(180)
            ServoMaticCommand.RIGHT -> rotoMatic.swing(0)
            ServoMaticCommand.CENTER -> rotoMatic.swing(90)
            ServoMaticCommand.SLEEP -> {
                ServoDisplay.sleep(true)
                SensorSuite.disabled = true
            }

            ServoMaticCommand.WAKEY -> {
                ServoDisplay.sleep(false)
                SensorSuite.disabled = false
            }
        }
    }
}

private val executor = Executors.newSingleThreadExecutor()
private val busy = AtomicBoolean(false)

var liftLast = false
fun ServoLinearActuator.isUp(): Boolean = current() > 80

fun main(args: Array<String>?) {
    // pass any arg and we'll use the remote pi
    // NOTE: this reqquires a diozero daemon running on the remote pi and the diozero remote jar in the classpath
    if (args?.isNotEmpty() == true) System.setProperty(REMOTE_PI, args[0])

    SensorSuite.start()
    ServoDisplay.sleep(false)
    ServoController().use { hat ->
        rotoServo = hat.getServo(0, ServoTrim(1500, 1100), 0)
        liftServo = hat.getServo(1, ServoTrim.TOWERPRO_SG90, 0)

        joinTopic(
            PROXIMITY_TOPIC,
            KobotsSubscriber<SensorSuite.ProximityTrigger> { trigger ->
                if (liftLast) {
                    liftoMatic.move(if (liftoMatic.current() == 0) 100 else 0)
                } else {
                    when (rotoMatic.current()) {
                        0 -> rotoMatic.swing(90)
                        90 -> rotoMatic.swing(180)
                        180 -> rotoMatic.swing(0)
                    }
                }
            }
        )

        doneLatch.await()
        logger.warn("Servomatic shutdown")
        rotoMatic.swing(0)
        while (busy.get()) KobotSleep.millis(100)
        liftoMatic.move(0)
    }
    logger.warn("Servomatic exit")
    ServoDisplay.sleep(true)
    SensorSuite.close()
    exitProcess(0)
}

class ServomaticEvent(val source: String = "servomatic", val rotoStatus: Int, val liftIsUp: Boolean)

private const val ROTO_SPEED = 50L

@Synchronized
fun ServoRotator.swing(target: Int) {
    if (busy.compareAndSet(false, true)) {
        executor.submit {
            while (!rotateTo(target)) {
                ServoDisplay.show(this.current(), liftoMatic.current())
                KobotSleep.millis(ROTO_SPEED)
            }
            liftLast = target == 0
            val event = ServomaticEvent(rotoStatus = this.current(), liftIsUp = liftoMatic.isUp())
            val jsonObject = JSONObject(event).toString()
            mqttClient.publish(EVENT_TOPIC, jsonObject)
            busy.set(false)
        }
    }
}

private const val LIFT_SPEED = 35L

@Synchronized
fun ServoLinearActuator.move(target: Int) {
    if (busy.compareAndSet(false, true)) {
        executor.submit {
            while (!extendTo(target)) {
                ServoDisplay.show(rotoMatic.current(), this.current())
                KobotSleep.millis(LIFT_SPEED)
            }
            liftLast = target != 0
            val jsonObject = JSONObject(ServomaticEvent(rotoStatus = rotoMatic.current(), liftIsUp = isUp()))
            mqttClient.publish(EVENT_TOPIC, jsonObject.toString())
            busy.set(false)
        }
    }
}
