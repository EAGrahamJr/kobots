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
    id("kobots-hardware-conventions")
    id("com.github.johnrengelman.shadow")
    application
}

dependencies {
    implementation("crackers.kobots:kobots-parts:$PARTS_VER")

    implementation("org.eclipse.paho:org.eclipse.paho.mqttv5.client:1.2.5")
    implementation("org.json:json:20230227")
}

tasks {
    /**
     * Build a "shadow jar" for single-runnable deployment
     */
    shadowJar {
        mustRunAfter("test")
        archiveBaseName.set(project.ext.get("jar.name").toString())
        archiveVersion.set("")
        archiveClassifier.set("")
        // this is important for using the remote client at the same time as other providers
        mergeServiceFiles()
    }
    // don't need any of these
    jar { enabled = false }
    startScripts { enabled = false }
    distTar { enabled = false }
    distZip { enabled = false }
    shadowDistTar { enabled = false }
    shadowDistZip { enabled = false }

    /**
     * Deploy said shadow-jar to a remote Pi for runtime fun
     */
    create("deployApp") {
        dependsOn(":${project.name}:shadowJar")
        doLast {
            val sshTarget = System.getProperty("remote", "marvin.local")
            val name = project.ext.get("jar.name").toString()

            println("Sending $name to $sshTarget")
            exec {
                commandLine(
                    "sh", "-c", """
                scp build/libs/$name.jar $sshTarget:/home/crackers
                scp ${rootDir}/*.sh $sshTarget:/home/crackers
                """.trimIndent()
                )
            }
        }
    }
}
