plugins {
    kotlin("jvm") version "1.7.20"
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
}
