plugins {
    id("application-conventions")
}

dependencies {
    implementation(project(":parts"))
    implementation("com.diozero:diozero-provider-pigpio:$DIOZERO_VER")
//    implementation("com.diozero:diozero-provider-remote:$DIOZERO_VER")
//    implementation("crackers.automation:hassk:0.0.1")
    implementation("org.json:json:20230227")
    // getting silly
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    // more HA craziness
    implementation("crackers.automation:hassk:0.0.1")
    implementation("com.typesafe:config:1.4.2")
}

project.ext.set("jar.name", "kobots-app")

application {
    mainClass.set("crackers.kobots.app.ThingieKt")
}
