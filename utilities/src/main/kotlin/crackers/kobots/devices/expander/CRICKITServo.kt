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

package crackers.kobots.devices.expander

import com.diozero.internal.spi.InternalServoDeviceInterface

class CRICKITServo(
    servoNumber: Int,
    seeSaw: AdafruitSeeSaw,
    seeSawPin: Int,
    frequency: Int,
    minPulseWidthUs: Int, maxPulseWidthUs: Int
) {
    private val delegate = InternalServo(
        "SERVO$servoNumber", servoNumber, seeSaw, seeSawPin, frequency,
        minPulseWidthUs, maxPulseWidthUs, minPulseWidthUs
    )
    private var myAngle = 0

    var angle: Int
        get() = myAngle
        set(value) {
            myAngle = value
            delegate.setFraction(value / 180f)
        }

    var frequencyHz: Int
        get() = delegate.servoFrequency
        set(value) {
            delegate.servoFrequency = value
        }
}


internal class InternalServo(
    private val key: String,
    private val servoNumber: Int,
    private val seeSaw: AdafruitSeeSaw,
    private val seeSawPin: Int,
    frequency: Int,
    private val minPulseWidthUs: Int,
    private val maxPulseWidthUs: Int,
    initialPulseWidthUs: Int
) : InternalServoDeviceInterface {

    private var pulseWidthUs: Int = 0

    private var minDutyCycle: Int = 0
    private var dutyRange: Int = 0
    private var frequencyHz: Int = 50
    private val pulseWidthRangeUs = maxPulseWidthUs - minPulseWidthUs

    init {
        setDutyRange()
        servoFrequency = frequency
        setPulseWidthUs(initialPulseWidthUs)
    }

    override fun close() {
        // does nothing here
    }

    override fun getKey() = key

    override fun isOpen() = true

    override fun isChild() = true

    override fun setChild(child: Boolean) {
        // ignored
    }

    override fun getGpio() = -1

    override fun getServoNum() = servoNumber

    override fun getPulseWidthUs() = pulseWidthUs
    override fun setPulseWidthUs(pulseWidthUs: Int) {
        this.pulseWidthUs = pulseWidthUs
        setFraction((pulseWidthUs - minPulseWidthUs).toFloat() / pulseWidthRangeUs)
    }

    override fun getServoFrequency() = frequencyHz
    override fun setServoFrequency(frequencyHz: Int) {
        seeSaw.setPWMFreq(seeSawPin.toByte(), frequencyHz.toShort())
        this.frequencyHz = frequencyHz
        setDutyRange()
    }

    internal fun setFraction(fraction: Float) {
        val dutyCycle = fraction * dutyRange + minDutyCycle
        seeSaw.analogWrite(seeSawPin.toByte(), Math.round(dutyCycle).toShort(), true)
    }

    private fun setDutyRange() {
        val freqUs = frequencyHz / 1_000_000f
        minDutyCycle = (minPulseWidthUs * freqUs * 0xFFFF).toInt()
        val maxDutyCycle = maxPulseWidthUs * freqUs * 0xFFFF
        dutyRange = (maxDutyCycle - minDutyCycle).toInt()
    }
}
