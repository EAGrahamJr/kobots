plugins {
    id("application-conventions")
}

dependencies {
    implementation(project(":operations"))
}

application {
    mainClass.set("crackers.kobots.app.AppKt")
}
