plugins {
    id("kobots-hardware-conventions")
    id("com.github.johnrengelman.shadow")
    application
}

dependencies {
    implementation(project(":utilities"))

    // set up specific libraries for deployment builds
    if (gradle.startParameter.taskNames.filter { it.contains("deploy") }.isNotEmpty()) {
        // requires root to run, but makes everything much faster
        implementation("com.diozero:diozero-provider-pigpio:$DIOZERO_VER")
        // just in case
        compileOnly("com.diozero:diozero-provider-remote:$DIOZERO_VER")
    } else
        implementation("com.diozero:diozero-provider-remote:$DIOZERO_VER")
}
