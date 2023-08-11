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

package crackers.kobots.app.execution

/**
 * Reducto absurdium.
 *
 * Effectively a re-write of the previous demos, but using an abstract base class. Note this only works for
 * non-Rotomatic targets.
 */
object EyeDropDemo : PickAndMove() {
    override val pickupSequenceName = "Pick Up EyeDrops"
    override val pickupElbow: Int = -5
    override val pickupExtender: Int = 65
    override val pickupWaist: Int = 0

    override val gripperGrab: Int = 90

    override val transportElbow: Int = 60

    override val targetSequenceName = "Return EyeDrops"
    override val targetElbow: Int = pickupElbow
    override val targetExtender: Int = 30
    override val targetWaist: Int = 90
}
