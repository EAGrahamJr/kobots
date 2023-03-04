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

import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.NEOPIXEL_BASE
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.NEOPIXEL_BUF
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.NEOPIXEL_BUF_LENGTH
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.NEOPIXEL_PIN
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.NEOPIXEL_SHOW
import crackers.kobots.devices.lighting.PixelBuf
import crackers.kobots.devices.to2Bytes
import crackers.kobots.devices.twoBytesAndBuffer
import kotlin.concurrent.withLock

/**
 * NeoPixel handling via the SeeSaw
 */
class NeoPixel internal constructor(
    private val seeSaw: AdafruitSeeSaw,
    numPixels: Int,
    deviceNumber: Byte,
    bitsPerPixel: Int = 3
) : PixelBuf(numPixels, "GRB" + if (bitsPerPixel == 4) "W" else "", autoWriteEnabled = true) {
    init {
        seeSaw.write(NEOPIXEL_BASE, NEOPIXEL_PIN, byteArrayOf(deviceNumber))
        seeSaw.writeShort(NEOPIXEL_BASE, NEOPIXEL_BUF_LENGTH, (bitsPerPixel * numPixels).toShort())
    }

    override fun sendBuffer(buffer: ByteArray) = seeSaw.lock.withLock {
        val step = OUTPUT_BUFFER_SIZE - 2

        buffer.toList().chunked(step).forEachIndexed { index, chunk: List<Byte> ->
            val offset = (index * step).to2Bytes()
            val outputBuffer = twoBytesAndBuffer(offset.first, offset.second, chunk.toByteArray())
            seeSaw.write(NEOPIXEL_BASE, NEOPIXEL_BUF, outputBuffer)
        }
        seeSaw.write(NEOPIXEL_BASE, NEOPIXEL_SHOW)
        Unit
    }

    companion object {
        internal const val CRICKIT_PIN = 20.toByte()
        internal const val CRICKIT_STATUS = 27.toByte()

        // magic buffer size as determined by Adafruit
        private const val OUTPUT_BUFFER_SIZE = 24
    }
}
