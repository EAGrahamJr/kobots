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

package crackers.kobots.app

import crackers.kobots.parts.app.KobotSleep
import org.json.JSONObject

fun doArmThing(payload: JSONObject) {
    if (payload.optString("sequence") == "ReturnPickup" && !payload.optBoolean("started")) {
        logger.info("Drops done")
        servoRequest(swirlyHome)
    }
}

fun doAlertThing(payload: JSONObject) {
    if (payload.optString("sequence") == "proxAlert") {
        if (payload.optBoolean("started")) {
            logger.info("Alerting")
            SuzerainOfServos.stop()
            KobotSleep.millis(50)
            servoRequest(wavyUp)
        } else {
            logger.info("Not alerting")
            servoRequest(wavyDown)
        }
    }
}
