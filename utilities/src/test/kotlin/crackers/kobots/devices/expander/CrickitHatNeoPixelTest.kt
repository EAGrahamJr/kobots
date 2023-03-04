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
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.NEOPIXEL_BUF_LENGTH
import crackers.kobots.devices.expander.AdafruitSeeSaw.Companion.NEOPIXEL_PIN
import crackers.kobots.devices.expander.NeoPixel.Companion.CRICKIT_PIN
import crackers.kobots.utilities.GOLDENROD
import crackers.kobots.utilities.PURPLE
import crackers.kobots.utilities.hex
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.awt.Color
import crackers.kobots.devices.MockI2CDevice.requests as mockRequests

/**
 * Direct usage of NeoPixel (PixelBuf) objects.
 */
class CrickitHatNeoPixelTest : FunSpec(
    {
        clearBeforeTest()
        val seeSaw = testHat

        context("Setup") {
            test("Strand of 30") {
                NeoPixel(seeSaw, 30, CRICKIT_PIN)
                mockRequests shouldContainExactly listOf(NEOPIXEL_BASE, NEOPIXEL_PIN, CRICKIT_PIN) +
                    listOf(NEOPIXEL_BASE, NEOPIXEL_BUF_LENGTH, 0x00, 90)
                println(mockRequests.joinToString("") { it.hex() })
            }
            test("Status") {
                // TODO
            }
        }

        context("Use a 30-strand") {
            val strand = NeoPixel(seeSaw, 30, CRICKIT_PIN)

            context("Fill with colors") {
                // because the output was capturred as text, this seems easier to validate with
                test("Fill with RED") {
                    strand.fill(Color.RED)
                    mockRequests.joinToString("") { it.hex().lowercase() } shouldBe
                        "0e04000000ff0000ff0000ff0000ff0000ff0000ff0000ff0000" +
                        "0e040016ff0000ff0000ff0000ff0000ff0000ff0000ff0000ff" +
                        "0e04002c0000ff0000ff0000ff0000ff0000ff0000ff0000ff00" +
                        "0e04004200ff0000ff0000ff0000ff0000ff0000ff0000ff0000" +
                        "0e040058ff00" +
                        "0e05"
                }
                test("Fill with GREEN") {
                    strand.fill(Color.GREEN)
                    mockRequests.joinToString("") { it.hex().lowercase() } shouldBe
                        "0e040000ff0000ff0000ff0000ff0000ff0000ff0000ff0000ff" +
                        "0e0400160000ff0000ff0000ff0000ff0000ff0000ff0000ff00" +
                        "0e04002c00ff0000ff0000ff0000ff0000ff0000ff0000ff0000" +
                        "0e040042ff0000ff0000ff0000ff0000ff0000ff0000ff0000ff" +
                        "0e0400580000" +
                        "0e05"
                }
                test("Fill with CYAN") {
                    strand.fill(Color.CYAN)
                    mockRequests.joinToString("") { it.hex().lowercase() } shouldBe
                        "0e040000ff00ffff00ffff00ffff00ffff00ffff00ffff00ffff" +
                        "0e04001600ffff00ffff00ffff00ffff00ffff00ffff00ffff00" +
                        "0e04002cffff00ffff00ffff00ffff00ffff00ffff00ffff00ff" +
                        "0e040042ff00ffff00ffff00ffff00ffff00ffff00ffff00ffff" +
                        "0e04005800ff" +
                        "0e05"
                }
                test("Fill with PURPLE") {
                    strand.fill(PURPLE)
                    mockRequests.joinToString("") { it.hex().lowercase() } shouldBe
                        "0e04000000b4ff00b4ff00b4ff00b4ff00b4ff00b4ff00b4ff00" +
                        "0e040016b4ff00b4ff00b4ff00b4ff00b4ff00b4ff00b4ff00b4" +
                        "0e04002cff00b4ff00b4ff00b4ff00b4ff00b4ff00b4ff00b4ff" +
                        "0e04004200b4ff00b4ff00b4ff00b4ff00b4ff00b4ff00b4ff00" +
                        "0e040058b4ff" +
                        "0e05"
                }
            }
            context("Dimming") {
                // N.B. value for blue is "one off" of the Python result, so "good enough"
                test("Dim the PURPLE") {
                    strand.fill(PURPLE)
                    mockRequests.clear()
                    strand.brightness = .1f
                    mockRequests.joinToString("") { it.hex().lowercase() } shouldBe
                        "0e04000000121a00121a00121a00121a00121a00121a00121a00" +
                        "0e040016121a00121a00121a00121a00121a00121a00121a0012" +
                        "0e04002c1a00121a00121a00121a00121a00121a00121a00121a" +
                        "0e04004200121a00121a00121a00121a00121a00121a00121a00" +
                        "0e040058121a" +
                        "0e05"
                }
            }

            context("Single pixel") {
                beforeTest {
                    strand.brightness = 1f
                    strand.fill(Color.RED)
                    mockRequests.clear()
                }

                test("Set a single pixel to YELLOW") {
                    strand[14] = GOLDENROD
                    mockRequests.joinToString("") { it.hex().lowercase() } shouldBe
                        "0e04000000ff0000ff0000ff0000ff0000ff0000ff0000ff0000" +
                        "0e040016ff0000ff0000ff0000ff0000ff0000ff0000ff0096ff" +
                        "0e04002c0000ff0000ff0000ff0000ff0000ff0000ff0000ff00" +
                        "0e04004200ff0000ff0000ff0000ff0000ff0000ff0000ff0000" +
                        "0e040058ff00" +
                        "0e05"
                }
                test("Set a single pixel to YELLOW while dimmed") {
                    strand.brightness = .1f
                    mockRequests.clear()

                    strand[14] = GOLDENROD
                    mockRequests.joinToString("") { it.hex().lowercase() } shouldBe
                        "0e040000001a00001a00001a00001a00001a00001a00001a0000" +
                        "0e0400161a00001a00001a00001a00001a00001a00001a000f1a" +
                        "0e04002c00001a00001a00001a00001a00001a00001a00001a00" +
                        "0e040042001a00001a00001a00001a00001a00001a00001a0000" +
                        "0e0400581a00" +
                        "0e05"
                }
            }
        }
    }
)
