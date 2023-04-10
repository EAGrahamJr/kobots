plugins {
    id("application-conventions")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.diozero:diozero-provider-remote:$DIOZERO_VER")
}

project.ext.set("jar.name", "marvin-pi")

application {
    // change this class to run other things on deployment
    mainClass.set("base.StuffKt")
}
