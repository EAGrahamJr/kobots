plugins {
    kotlin("jvm")
    id("org.jmailen.kotlinter")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("ch.qos.logback:logback-classic:1.2.7")

    testImplementation("io.kotest:kotest-runner-junit5:5.5.4")
    testImplementation("io.mockk:mockk:1.13.3")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of("17"))
    }
}

kotlinter {
    ignoreFailures = true
    disabledRules = arrayOf("no-wildcard-imports")
}

tasks {
    check {
//        dependsOn("installKotlinterPrePushHook")
        dependsOn("formatKotlin")
    }
    test {
        useJUnitPlatform()
    }
}
