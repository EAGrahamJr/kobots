plugins {
    id("common-conventions")
    `java-library`
}

dependencies {
    implementation("com.diozero:diozero-core:$DIOZERO_VER")
    implementation("crackers.kobots:kobots-devices:$DEVICES_VER") {
        exclude(group = "ch.qos.logback")
    }
}
