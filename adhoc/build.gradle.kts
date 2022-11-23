plugins {
    id("application-conventions")
}

tasks {
    shadowJar {
        archiveBaseName.set("marvin-pi")
        archiveVersion.set("")
        archiveClassifier.set("")
    }
    create("deployMe") {
        mustRunAfter("shadowJar")
        doLast {
            exec {
                commandLine(
                    "sh", "-c", """
                    scp build/libs/marvin-pi.jar marvin.local:/home/crackers
                    scp *.sh marvin.local:/home/crackers
                    """.trimIndent()
                )
            }
        }
    }
}

application {
    mainClass.set("FreenoveKt")
}

defaultTasks("clean", "shadowJar", "deployMe")
