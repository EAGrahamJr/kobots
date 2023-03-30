plugins {
    id("application-conventions")
}

dependencies {

}

val sshTarget = System.getProperty("remote", "marvin.local")
val JAR_NAME = "kobots-app"

tasks {
    shadowJar {
        archiveBaseName.set(JAR_NAME)
        archiveVersion.set("")
        archiveClassifier.set("")
        // this is important for sing the remote client at the same time as other providers
        mergeServiceFiles()
    }
    create("deployApp") {
        dependsOn(":app:shadowJar")
        doLast {
            println("Sending to $sshTarget")
            exec {
                commandLine(
                    "sh", "-c", """
                    scp build/libs/$JAR_NAME.jar $sshTarget:/home/crackers
                    scp *.sh $sshTarget:/home/crackers
                    """.trimIndent()
                )
            }
        }
    }
}

application {
    mainClass.set("crackers.kobots.app.DumbWaiterKt")
}
