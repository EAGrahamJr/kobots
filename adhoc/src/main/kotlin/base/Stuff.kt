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
package base

import crackers.kobots.utilities.elapsed
import java.time.Instant

infix fun Int.minutes(block: () -> Unit) {
    val start = Instant.now()
    val end = this * 60
    while (start.elapsed().seconds < end) {
        block()
    }
}

const val REMOTE_PI = "diozero.remote.hostname"

fun Float.stringify(digits: Int = 5) = String.format("%$digits.2f", this)
