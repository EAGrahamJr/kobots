plugins {
    id("application-conventions")
}

dependencies {
    implementation("com.pi4j:pi4j.core:2.2.1")
    implementation("com.pi4j:pi4j-plugin-raspberrypi:2.2.1")
    implementation("com.pi4j:pi4j-plugin-pigpio:2.2.1")
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
    mainClass.set("dork.DorkOneKt")
}

defaultTasks("clean", "shadowJar", "deployMe")
