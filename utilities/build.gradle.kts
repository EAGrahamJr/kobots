plugins {
    id("kobots-hardware-conventions")
    id("library-publish")
}

version = "0.0.1"

dependencies {
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    testImplementation("com.diozero:diozero-provider-mock:$DIOZERO_VER")
}
