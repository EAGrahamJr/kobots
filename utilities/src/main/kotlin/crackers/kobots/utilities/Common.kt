/*
 * Copyright 2022-2022 by E. A. Graham, Jr.
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

import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Ignore exceptions, for those occasions where you truly don't care.
 */
fun <R : Any> ignoreErrors(block: () -> R?): Optional<R> =
    try {
        Optional.ofNullable(block())
    } catch (_: Throwable) {
        // ignored
        Optional.empty()
    }

/**
 * Elapsed time.
 */
fun Instant.elapsed() = Duration.between(this, Instant.now())

/**
 * Short form to get unsigned hex strings
 */
fun Int.hex() = Integer.toHexString(this)
