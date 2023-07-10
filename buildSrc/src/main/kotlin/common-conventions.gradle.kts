plugins {
    kotlin("jvm")
    id("org.jmailen.kotlinter")
    idea
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.2.7")

    testImplementation("io.kotest:kotest-runner-junit5:5.6.2")
    testImplementation("io.mockk:mockk:1.13.5")
}

group = "crackers.kobots"

kotlin {
    jvmToolchain(17)
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
