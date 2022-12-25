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

package devices

import crackers.kobots.devices.ADS7830
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ADS7830Test {
    @Test
    fun `basic channel select for read`() {
        listOf(0x84, 0xc4, 0x94, 0xd4, 0xa4, 0xe4, 0xb4, 0xf4).forEachIndexed { index, value ->
            assertEquals(value, ADS7830.channelToRegister(index), "Channel $index did not match")
        }
    }
}
