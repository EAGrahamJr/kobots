plugins {
    id("application-conventions")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    //    implementation("com.diozero:diozero-imu-devices:$DIOZERO_VER")
}

val sshTarget = System.getProperty("remote", "marvin.local")

tasks {
    shadowJar {
        archiveBaseName.set("marvin-pi")
        archiveVersion.set("")
        archiveClassifier.set("")
        // this is important for sing the remote client at the same time as other providers
        mergeServiceFiles()
    }
    // shoot it to target
    create("deployMe") {
        dependsOn(":adhoc:shadowJar")
        doLast {
            println("Sending to $sshTarget")
            exec {
                commandLine(
                    "sh", "-c", """
                    scp build/libs/marvin-pi.jar $sshTarget:/home/crackers
                    scp *.sh $sshTarget:/home/crackers
                    """.trimIndent()
                )
            }
        }
    }
}

application {
    // change this class to run other things on deployment
    mainClass.set("base.StuffKt")
}
