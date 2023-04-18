plugins {
    id("application-conventions")
}

dependencies {
    implementation("com.diozero:diozero-provider-pigpio:$DIOZERO_VER")
    implementation("crackers.automation:hassk:1.0-SNAPSHOT")
}

project.ext.set("jar.name", "kobots-app")

application {
    mainClass.set("crackers.kobots.app.DumbWaiterKt")
}
