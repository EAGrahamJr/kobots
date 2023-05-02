plugins {
    kotlin("jvm") version "1.8.20"
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenLocal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.20-RC")
    implementation("com.github.johnrengelman:shadow:8.1.1")
    implementation("org.jmailen.gradle:kotlinter-gradle:3.12.0")
    // TODO uncomment if there's something to publish
//    implementation("crackers.buildstuff:crackers-gradle-plugins:1.0.0")
}
