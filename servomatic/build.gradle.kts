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

plugins {
    id("application-conventions")
}

dependencies {
    implementation(project(":robot"))
//    implementation("com.diozero:diozero-provider-pigpio:$DIOZERO_VER")
//    implementation("com.diozero:diozero-provider-remote:$DIOZERO_VER")
}

project.ext.set("jar.name", "servomatic")

application {
    mainClass.set("crackers.kobots.app.ServoThingKt")
}

tasks {
    startScripts { enabled = false }
}
