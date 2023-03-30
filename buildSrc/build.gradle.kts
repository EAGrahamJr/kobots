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
    implementation("com.github.johnrengelman:shadow:8.1.1")
    implementation("org.jmailen.gradle:kotlinter-gradle:3.12.0")
    implementation("crackers.buildstuff:crackers-gradle-plugins:1.0.0")
}
