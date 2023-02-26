plugins {
    id("application-conventions")
}

dependencies {
    if (gradle.startParameter.taskNames.contains("deployMe")) {
        println("Using PIGPIO")
        // requires root to run, but makes everything much faster
        implementation("com.diozero:diozero-provider-pigpio:$DIOZERO_VER")
        // just in case
        // TODO needs to be implemenation for the "relay" app to work
        compileOnly("com.diozero:diozero-provider-remote:$DIOZERO_VER")
    } else
        implementation("com.diozero:diozero-provider-remote:$DIOZERO_VER")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4-native-mt")
}

val sshTarget = "marvin.local"
//val sshTarget = "useless.local"

tasks {
    shadowJar {
        archiveBaseName.set("marvin-pi")
        archiveVersion.set("")
        archiveClassifier.set("")
        // this is important for sing the remote client at the same time as other providers
        mergeServiceFiles()
    }
    create("deployMe") {
        mustRunAfter("shadowJar")
        doLast {
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
