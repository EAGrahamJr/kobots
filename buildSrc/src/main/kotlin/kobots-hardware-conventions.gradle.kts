plugins {
    id("common-conventions")
    `java-library`
}

dependencies {
    implementation("com.diozero:diozero-core:$DIOZERO_VER")
    implementation("crackers.kobots:kobots-devices:0.0.1")
}
