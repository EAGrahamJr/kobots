plugins {
    id("kobots-hardware-conventions")
    id("library-publish")
}

version = "0.0.1"

dependencies {
    testImplementation("com.diozero:diozero-provider-mock:$DIOZERO_VER")
}
