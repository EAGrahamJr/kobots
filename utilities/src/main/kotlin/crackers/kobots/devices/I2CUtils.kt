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

package crackers.kobots.devices

import com.diozero.api.I2CDevice
import java.time.Duration
import kotlin.system.exitProcess

/**
 * Kill button on enabled Qwiic pHAT v2.0 (https://www.sparkfun.com/products/15945). The default consumer exits the
 * application with code 3. (Included here because Qwiic is I2C)
 */
val qwiicKill by lazy {
    DebouncedButton(17, Duration.ofMillis(50)).apply {
        whenPressed {
            System.err.println("Exiting process on qwiicKill")
            exitProcess(3)
        }
    }
}

/**
 * Creates a read/write sub-register (e.g. bit-mapped values). The values read/write are based on a bit-mask - e.g.
 * this will extract and write only a _subset_ of a register value. This is for those occassions that an I2C register
 * contains multiple values.
 *
 * - the `read` operation will shift the value "down" to provide the value
 * - the `write` operation will shift the value "up" to its proper bit-location and not change or influence other
 * bits in the register
 */
interface I2CSubRegister<N : Number> {
    val mask: Int
    fun read(): N
    fun write(data: N)
}

/**
 * More or less concrete implemenation of the sub-register.
 */
private abstract class SubRegister<N : Number>(final override val mask: Int) : I2CSubRegister<N> {
    protected abstract fun readRegister(): N
    protected abstract fun writeRegister(data: N)

    private val bits: Int
    private val offset: Int

    init {
        var shifter = mask
        var shiftCount = 0
        while (shifter and 0x01 == 0) {
            shiftCount++
            shifter = shifter shr 1
        }
        offset = shiftCount
        var bitCount = 0
        while (shifter and 0x01 == 1) {
            bitCount++
            shifter = shifter shr 1
        }
        bits = bitCount
    }

    override fun read(): N {
        val v = readRegister().toInt() shr offset
        return (v and (1 shl bits) - 1) as N
    }

    override fun write(data: N) {
        // create the mask
        var mask = (1 shl bits) - 1
        // mask off data
        val d = data.toInt() and mask

        // remove current data
        mask = mask shl offset
        val v = readRegister().toInt() and mask.inv()

        // add in new data
        val writeThis = (v or (d shl offset)) as N
        writeRegister(writeThis)
    }
}

/**
 * Read and write from 16-bit registers (short) using a mask to extract/write values.
 */
fun I2CDevice.shortSubRegister(register: Int, mask: Int): I2CSubRegister<Short> {
    val i2c = this
    return object : SubRegister<Short>(mask) {
        override fun readRegister(): Short = i2c.readWordData(register)
        override fun writeRegister(data: Short) = i2c.writeWordData(register, data)
    }
}
