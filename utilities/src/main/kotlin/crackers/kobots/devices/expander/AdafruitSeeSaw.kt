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

import com.diozero.api.DeviceInterface
import com.diozero.api.DigitalInputDevice
import com.diozero.api.I2CDevice
import com.diozero.api.RuntimeIOException
import com.diozero.util.SleepUtil
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.SignalMode.*
import crackers.kobots.devices.toBytes
import crackers.kobots.devices.twoBytesAndBuffer
import crackers.kobots.utilities.*
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.experimental.and

/**
 * An I2C to whatever helper chip. Note that this chip does **not** use "normal" I2C registers, but rather selects
 * "registry" locations with write before read.
 *
 * Based on the Adafruit [CircuitPython library](https://github.com/adafruit/Adafruit_CircuitPython_seesaw) and the
 * [Arduino library](https://github.com/adafruit/Adafruit_Seesaw)
 *
 * See [Using the SeeSaw](https://learn.adafruit.com/adafruit-seesaw-atsamd09-breakout?view=all#using-the-seesaw-platform)
 *
 * The flow-control device is available, but typically not used.
 */

class AdafruitSeeSaw(
    private val i2CDevice: I2CDevice,
    private val readyOutput: DigitalInputDevice? = null,
    val initReset: Boolean = true
) : DeviceInterface {

    /**
     * Analog pin inputs. This must be set for [analogRead] to work.
     */
    lateinit var analogInputPins: IntArray

    /**
     * Analog PWM (servo) pin outputs. This must be set for [analogWrite] to work.
     */
    lateinit var pwmOutputPins: IntArray

    private val logger = LoggerFactory.getLogger("SeeSaw")
    val chipId: Int

    init {
        // reset
        if (initReset) softwareReset()

        chipId = readByte(STATUS_BASE, STATUS_HW_ID).toInt() and 0xFF
        if (chipId != DeviceType.ATTINY8X7_HW_ID_CODE.pid && chipId != DeviceType.SAMD09_HW_ID_CODE.pid) {
            throw RuntimeIOException(String.format("Seesaw Hardware ID ${chipId.hex()} is not correct."))
        }

        // TODO this is currently not really used in this code - python code uses it to over-load pin-maps
        val pid = getVersion().toInt() shr 16
    }

    override fun close() {
        readyOutput?.close()
        i2CDevice.close()
    }

    fun getOptions(): Int {
        TODO("Not yet")
    }

    fun getVersion() = readInt(STATUS_BASE, STATUS_VERSION)

    fun getProdDatecode(pid: Short, year: Byte, mon: Byte, day: Byte): Boolean {
        TODO("Not yet")
    }

    fun softwareReset(delay: Duration = Duration.ofMillis(500)) {
        writeByte(STATUS_BASE, STATUS_SWRST, 0xFF.toByte())
        SleepUtil.sleepMillis(delay.toMillis())
    }

    // Digital modes --------------------------------------------------------------------------------------------------
    fun pinMode(pin: Int, mode: SignalMode) {
        val selector = pin.digitalPin()

        when (mode) {
            INPUT -> {
                write(GPIO_BASE, GPIO_DIRECTION_INPUT, selector)
                write(GPIO_BASE, GPIO_PULL_RESISTOR_DISABLED, selector)
            }

            OUTPUT -> write(GPIO_BASE, GPIO_DIRECTION_OUTPUT, selector)
            INPUT_PULLUP -> {
                write(GPIO_BASE, GPIO_DIRECTION_INPUT, selector)
                write(GPIO_BASE, GPIO_PULL_RESISTOR_ENABLED, selector)
                write(GPIO_BASE, GPIO_BULK_SET, selector)
            }

            INPUT_PULLDOWN -> {
                write(GPIO_BASE, GPIO_DIRECTION_INPUT, selector)
                write(GPIO_BASE, GPIO_PULL_RESISTOR_ENABLED, selector)
                write(GPIO_BASE, GPIO_BULK_CLEAR, selector)
            }
        }
    }

    /**
     * Write an on/off value to a digital I/O port.
     */
    fun digitalWrite(pin: Int, value: Boolean) {
        val selector = pin.digitalPin()
        val registerCommand = if (value) GPIO_BULK_SET else GPIO_BULK_CLEAR
        write(GPIO_BASE, registerCommand, selector)
    }

    /**
     * Read an on/off value from a digital I/O port.
     */
    fun digitalRead(pin: Int): Boolean {
        val bPort = pin >= 32
        val (size, shift) = if (bPort) Pair(8, pin - 32) else Pair(4, pin)
        val bitMask = 1 shl shift
        return read(GPIO_BASE, GPIO_BULK, size).let {
            // TODO figure out little endian stuff?
            // use the last two bytes for the data
            byteArrayOf(it[size - 2], it[size - 1]).toShort() and bitMask != 0
        }
    }

    fun digitalRead(ports: IntArray): Map<Int, Boolean> {
        // N.B. note that reading 8 bytes in the `digitalRead` function
        // **includes** the A port as the first 4 bytes
        // this seems like it should be created as a class?
        TODO("Enhancement that does not appear in the Adafruit code")
    }

    fun setGPIOInterrupts(pins: Int, enabled: Boolean) {
        TODO("Not yet")
    }

    private fun Int.digitalPin(): ByteArray {
        val bMode = this >= 32
        val pinValue = 1 shl (if (bMode) this - 32 else this)
        return if (bMode) writeToByteArray(pinValue, 8, 4) else writeToByteArray(pinValue, 4, 0)
    }

    /**
     * Read value from [pin] as an Int (to avoid any potential negative values).
     */
    @Throws(UnsupportedOperationException::class, IllegalArgumentException::class)
    fun analogRead(pin: Byte): Int {
        if (!::analogInputPins.isInitialized) {
            throw UnsupportedOperationException("No analog input pins defined for device.")
        }
        val offset = analogInputPins.pwmOffsets(pin)
        return read(ADC_BASE, (ADC_CHANNEL_OFFSET + offset).toByte(), 2).toShort()
    }

    // PWM outputs ----------------------------------------------------------------------------------------------------
    /**
     * Write value to [pin] as a PWM value
     */
    @Throws(UnsupportedOperationException::class, IllegalArgumentException::class)
    fun analogWrite(pin: Byte, value: Short, twoBytes: Boolean = true) {
        if (!::pwmOutputPins.isInitialized) {
            throw UnsupportedOperationException("No analog output pins defined for device.")
        }
        val offset = pwmOutputPins.pwmOffsets(pin)

        val bytes = if (twoBytes) {
            byteArrayOf(offset) + value.toByteArray()
        } else {
            byteArrayOf(offset, (value and 0xFF).toByte())
        }

        write(TIMER_BASE, TIMER_PWM, bytes)
    }

    /**
     * Setting the PWM frequency goes hand-in-hand with the [analogWrite] function (same pins). Together, these values
     * set the basic pulsed output from the PWM pins on the device.
     */
    @Throws(UnsupportedOperationException::class, IllegalArgumentException::class)
    fun setPWMFreq(pin: Byte, freq: Short) {
        if (!::pwmOutputPins.isInitialized) {
            throw UnsupportedOperationException("No analog output pins defined for device.")
        }
        val offset = pwmOutputPins.pwmOffsets(pin)
        val cmd = byteArrayOf(offset) + freq.toByteArray()

        write(TIMER_BASE, TIMER_FREQ, cmd)
    }

    /**
     * Hardware chip determines if a straight "offset" is used or a pin-mapping
     */
    private fun IntArray.pwmOffsets(pinToFind: Byte): Byte = indexOf(pinToFind.toInt()).let {
        if (it < 0) throw IllegalArgumentException("Unknown analog pin ${pinToFind.hex()}")
        // hardware differences (this is interesting)
        when (chipId) {
            DeviceType.SAMD09_HW_ID_CODE.pid -> it
            DeviceType.ATTINY8X7_HW_ID_CODE.pid -> pinToFind
            else -> throw IllegalStateException("Unknown chip ${chipId.hex()} - this should not happen!")
        }
    }.toByte()

    // Touchpads ------------------------------------------------------------------------------------------------------
    fun touchRead(pin: Int): Int = readShort(TOUCH_BASE, (TOUCH_CHANNEL_OFFSET + pin).toByte())

    // Other ----------------------------------------------------------------------------------------------------------

    fun enableSercomDataRdyInterrupt(sercom: Byte = 0) {
        TODO("Not yet")
    }

    fun disableSercomDataRdyInterrupt(sercom: Byte = 0) {
        TODO("Not yet")
    }

    fun readSercomData(sercom: Byte = 0): Byte {
        TODO("Not yet")
    }

    fun EEPROMWrite8(addr: Byte, value: Byte) {
        TODO("Not yet")
    }

    fun EEPROMWrite(addr: Byte, buffer: ByteArray, size: Byte) {
        TODO("Not yet")
    }

    fun EEPROMRead8(addr: Byte): Byte {
        TODO("Not yet")
    }

    fun setI2CAddr(addr: Byte) {
        TODO("Not yet")
    }

    fun getI2CAddr(): Byte {
        TODO("Not yet")
    }

    fun UARTSetBaud(baud: Int) {
        TODO("Not yet")
    }

    fun setKeypadEvent(key: Byte, edge: Byte, enable: Boolean = true) {
        TODO("Not yet")
    }

    fun enableKeypadInterrupt() {
        TODO("Not yet")
    }

    fun disableKeypadInterrupt() {
        TODO("Not yet")
    }

    fun getKeypadCount(): Byte {
        TODO("Not yet")
    }

    fun readKeypad(buffer: ByteArray, count: Byte) {
        TODO("Not yet")
    }

    fun getTemp(): Float {
        TODO("Not yet")
    }

    fun getEncoderPosition(encoder: Byte = 0): Int {
        TODO("Not yet")
    }

    fun getEncoderDelta(encoder: Byte = 0): Int {
        TODO("Not yet")
    }

    fun enableEncoderInterrupt(encoder: Byte = 0): Boolean {
        TODO("Not yet")
    }

    fun disableEncoderInterrupt(encoder: Byte = 0): Boolean {
        TODO("Not yet")
    }

    fun setEncoderPosition(pos: Int, encoder: Byte = 0) {
        TODO("Not yet")
    }

//    virtual size_t write(:Byte){ TODO("Not yet") }
//    virtual size_t write(const char *str){ TODO("Not yet") }

    /**
     * Write a [value] into an allocated array at an [offset].
     */
    private fun writeToByteArray(value: Int, size: Int, offset: Int) =
        ByteArray(size).apply {
            set(offset, (value shr 24).toByte())
            set(offset + 1, ((value shr 16) and BYTE_MASK).toByte())
            set(offset + 2, ((value shr 8) and BYTE_MASK).toByte())
            set(offset + 3, (value and BYTE_MASK).toByte())
        }

    private fun readInt(registerBase: Byte, register: Byte) = read(registerBase, register, 4).toLong()

    private fun readShort(registerBase: Byte, register: Byte): Int = read(registerBase, register, 2).toShort()

    private fun readByte(registerBase: Byte, register: Byte): Byte = read(registerBase, register, 1)[0]

    private fun writeByte(registerBase: Byte, register: Byte, value: Byte): Boolean {
        return write(registerBase, register, byteArrayOf(value))
    }

    internal fun writeShort(registerBase: Byte, register: Byte, value: Short) {
        val (hiByte, loByte) = value.toBytes()
        write(registerBase, register, byteArrayOf(hiByte, loByte))
    }

    /**
     * Read an arbitrary I2C register range from the device
     */
    @Synchronized
    internal fun read(
        registerBase: Byte,
        register: Byte,
        bytesToRead: Int,
        delay: Duration = Duration.ofMillis(8)
    ): ByteArray {
        // set the register and wait for ready or just wait
        write(registerBase, register)
        readyOutput?.apply {
            while (!value) Thread.onSpinWait()
        } ?: SleepUtil.busySleep(delay.toNanos())
        // read what we got
        return i2CDevice.readBytes(bytesToRead).also { ba ->
            ba.debug("Read")
        }
    }

    /**
     * Write an arbitrary I2C register data to the device
     */
    @Synchronized
    internal fun write(registerBase: Byte, register: Byte, buffer: ByteArray = ByteArray(0)): Boolean {
        val output = twoBytesAndBuffer(registerBase, register, buffer)

        if (buffer.isEmpty()) output.debug("Register") else output.debug("Wrote")

        readyOutput?.apply {
            while (!value) Thread.onSpinWait()
        }

        i2CDevice.writeBytes(*output)
        return true
    }

    private fun ByteArray.debug(prefix: String) {
        logger.debug("$prefix ${hex()}")
    }

    companion object {
        const val STATUS_BASE = 0x00.toByte()

        const val SERCOM0_BASE = 0x02

        const val DAC_BASE = 0x0A
        const val INTERRUPT_BASE = 0x0B
        const val DAP_BASE = 0x0C
        const val EEPROM_BASE = 0x0D
        const val NEOPIXEL_BASE = 0x0E.toByte()
        const val TOUCH_BASE = 0x0F.toByte()
        const val ENCODER_BASE = 0x11

        const val GPIO_BASE = 0x01.toByte()
        const val GPIO_DIRECTION_OUTPUT = 0x02.toByte()
        const val GPIO_DIRECTION_INPUT = 0x03.toByte()
        const val GPIO_BULK = 0x04.toByte()
        const val GPIO_BULK_SET = 0x05.toByte()
        const val GPIO_BULK_CLEAR = 0x06.toByte()
        const val GPIO_BULK_TOGGLE = 0x07
        const val GPIO_INTENSET = 0x08
        const val GPIO_INTENCLR = 0x09
        const val GPIO_INTFLAG = 0x0A
        const val GPIO_PULL_RESISTOR_ENABLED = 0x0B.toByte()
        const val GPIO_PULL_RESISTOR_DISABLED = 0x0C.toByte()

        const val STATUS_HW_ID = 0x01.toByte()
        const val STATUS_VERSION = 0x02.toByte()
        const val STATUS_OPTIONS = 0x03
        const val STATUS_TEMP = 0x04
        const val STATUS_SWRST = 0x7F.toByte()

        // PWM Timers
        const val TIMER_BASE = 0x08.toByte()
        const val TIMER_STATUS = 0x00
        const val TIMER_PWM = 0x01.toByte()
        const val TIMER_FREQ = 0x02.toByte()

        const val ADC_BASE = 0x09.toByte()
        const val ADC_STATUS = 0x00
        const val ADC_INTEN = 0x02
        const val ADC_INTENCLR = 0x03
        const val ADC_WINMODE = 0x04
        const val ADC_WINTHRESH = 0x05
        const val ADC_CHANNEL_OFFSET = 0x07.toByte()

        const val SERCOM_STATUS = 0x00
        const val SERCOM_INTEN = 0x02
        const val SERCOM_INTENCLR = 0x03
        const val SERCOM_BAUD = 0x04
        const val SERCOM_DATA = 0x05

        const val NEOPIXEL_STATUS = 0x00
        const val NEOPIXEL_PIN = 0x01.toByte()
        const val NEOPIXEL_SPEED = 0x02
        const val NEOPIXEL_BUF_LENGTH = 0x03.toByte()
        const val NEOPIXEL_BUF = 0x04.toByte()
        const val NEOPIXEL_SHOW = 0x05.toByte()

        const val TOUCH_CHANNEL_OFFSET = 0x10.toByte()

        const val EEPROM_I2C_ADDR = 0x3F

        const val ENCODER_STATUS = 0x00
        const val ENCODER_INTENSET = 0x10
        const val ENCODER_INTENCLR = 0x20
        const val ENCODER_POSITION = 0x30
        const val ENCODER_DELTA = 0x40

        enum class SignalMode {
            INPUT, OUTPUT, INPUT_PULLUP, INPUT_PULLDOWN
        }

        enum class DeviceType(val pid: Int) {
            // TODO: update when we get real PID (hasn't happened after several years - shrug)
            CRICKIT_PID(9999),
            ROBOHATMM1_PID(9998),
            SAMD09_HW_ID_CODE(0x55),
            ATTINY8X7_HW_ID_CODE(0x87)
        }
    }
}
