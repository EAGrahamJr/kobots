plugins {
    id("application-conventions")
}

dependencies {
    // TODO make this configurable somehow
//    implementation("com.diozero:diozero-provider-remote:1.3.5")
    // requires root to run, but makes everything much faster
    implementation("com.diozero:diozero-provider-pigpio:1.3.5")
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
//    mainClass.set("freenovekit.FreenoveKt")
//    mainClass.set("dork.DorkOneKt")
//    mainClass.set("lcd.LCDTestingKt")
//    mainClass.set("dork.CatBonker1Kt")
//    mainClass.set("freenovekit.SoftPWMJitterTestKt")
    mainClass.set("dork.CB1ThreadsKt")
}

defaultTasks("clean", "shadowJar", "deployMe")
