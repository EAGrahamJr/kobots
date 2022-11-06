plugins {
    id("application-conventions")
}

dependencies {
    implementation(project(":operations"))
    implementation(project(":utilities"))
}

application {
    mainClass.set("crackers.kobots.app.AppKt")
}
