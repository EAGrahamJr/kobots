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

package crackers.kobots.utilities

import com.diozero.util.Hex

/**
 * This has probably been defined a million ways in that many variations.
 */
const val BYTE_MASK = 0x00FF

fun ByteArray.toShort(): Int = ((get(0).toInt() and BYTE_MASK) shl 8) + (get(1).toInt() and BYTE_MASK)
fun ByteArray.toLong(): Long = mapIndexed { index, byte ->
    ((byte.toInt() and BYTE_MASK) shl (3 - index) * 8).toLong()
}.sum()

fun Short.toByteArray() = toInt().let {
    val upper = it and 0xFF00
    val lower = it and 0x00FF
    byteArrayOf((upper shr 8).toByte(), lower.toByte())
}

/**
 * Short form to get unsigned hex strings
 */
fun Int.hex(): String = Hex.encode(this)
fun Short.hex(): String = Hex.encode(this)
fun Byte.hex(): String = Hex.encode(this)
fun ByteArray.hex(): String = joinToString(separator = "") { it.hex() }
