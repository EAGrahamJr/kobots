plugins {
    id("common-conventions")
    `java-library`
}

dependencies {
    implementation("com.diozero:diozero-core:$DIOZERO_VER")
    implementation("com.diozero:diozero-imu-devices:$DIOZERO_VER")
}
