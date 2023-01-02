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

package dork

import com.diozero.devices.LcdInterface
import com.diozero.devices.SimpleLcd
// import com.diozero.devices.imu.invensense.MPU6050
import java.lang.Thread.sleep

fun main() {
}

// fun `put me down`() {
//    val sensor = MPU6050(1)
//    TC1604Lcd().apply {
//        displayText("Test starting")
// //        setText(1, "Calibrating...")
// //        val gyroCalibration = sensor.calibrateGyro(50)
//
//        var lastZ = sensor.accelerometerData.z
//        2 minutes {
//            val nowZ = sensor.accelerometerData.z
//            val diff = nowZ - lastZ
//            lastZ = nowZ
//            setText(1, "Diff ${String.format("%+.2f", diff)}")
//            sleep(500)
//        }
//    }.close()
//    sensor.close()
// }

fun shiftSilly() {
    sleep(1000)
    SimpleLcd().apply {
        createChar(0, LcdInterface.Characters.get("smilie"))
        setText(0, "Hi, Roger! " + 0.toChar())
        sleep(2000)
        for (i in 1..16) {
            shiftDisplayLeft()
            sleep(250)
        }
        clear()
        returnHome()
        displayText("Wanna do lunch\nnext week?")
        sleep(3000)
    }.close()
}
