/*
 * Copyright 2022-2024 by E. A. Graham, Jr.
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

package crackers.kobots.app.newarm


/**
 * Canned, known positions
 */
object Position {
    val waitForDropOff = DumbFunc.ArmLocation(
        swing = 90,
        boom = 50,
        arm = 90,
        bucket = 85
    )
    val first = DumbFunc.ArmLocation(
        swing = 0,
        boom = 70,
        arm = 75,
        bucket = 50
    )
}
