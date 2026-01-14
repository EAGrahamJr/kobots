/*
 * Copyright 2022-2026 by E. A. Graham, Jr.
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

import crackers.buildstuff.semver.SimpleSemverVersion

plugins {
    kotlin("jvm")
//    id("org.jmailen.kotlinter")
    idea
    id("crackers.buildstuff.simple-semver")
    id("com.github.ben-manes.versions")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation("io.kotest:kotest-runner-junit5:6.0.3")
    testImplementation("io.mockk:mockk:1.14.5")
}

group = "crackers.kobots"

// set the project version to the semver version
val semver: Provider<SimpleSemverVersion> by project
version = semver.get().toString()

kotlin {
    jvmToolchain(21)
}

//kotlinter {
//    ignoreFailures = true
//    disabledRules = arrayOf("no-wildcard-imports")
//}

tasks {
    check {
//        dependsOn("installKotlinterPrePushHook")
//        dependsOn("formatKotlin")
    }
    test {
        useJUnitPlatform()
    }
}
