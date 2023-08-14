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

plugins {
    id("application-conventions")
}

dependencies {
    implementation(project(":parts"))
//    implementation("com.diozero:diozero-provider-pigpio:$DIOZERO_VER")
//    implementation("com.diozero:diozero-provider-remote:$DIOZERO_VER")
    implementation("org.json:json:20230227")
    // getting silly
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    // more HA craziness
    implementation("crackers.automation:hassk:0.0.1") {
        exclude(group = "ch.qos.logback")
    }
    implementation("com.typesafe:config:1.4.2")
}

project.ext.set("jar.name", "rotomatic")

application {
    mainClass.set("crackers.kobots.app.RotoThingKt")
}
