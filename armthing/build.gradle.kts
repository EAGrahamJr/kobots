plugins {
    id("application-conventions")
}

dependencies {
    implementation("com.diozero:diozero-provider-pigpio:$DIOZERO_VER")
//    implementation("com.diozero:diozero-provider-remote:$DIOZERO_VER")
    implementation("crackers.automation:hassk:0.0.1")
}

project.ext.set("jar.name", "kobots-app")

application {
    mainClass.set("crackers.kobots.app.ThingieKt")
}
