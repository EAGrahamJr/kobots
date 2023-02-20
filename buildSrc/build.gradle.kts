plugins {
    kotlin("jvm") version "1.8.10"
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenLocal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
    implementation("org.jmailen.gradle:kotlinter-gradle:3.12.0")
    implementation("crackers.buildstuff:crackers-gradle-plugins:1.0.0")
}
