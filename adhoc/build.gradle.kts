plugins {
    id("application-conventions")
}

dependencies {
    if (gradle.startParameter.taskNames.contains("deployMe")) {
        println("Using PIGPIO")
        // requires root to run, but makes everything much faster
        implementation("com.diozero:diozero-provider-pigpio:$DIOZERO_VER")
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
//    mainClass.set("freenovekit.FreenoveKt")
//    mainClass.set("dork.DorkOneKt")
//    mainClass.set("lcd.LCDTestingKt")
//    mainClass.set("dork.CatBonker1Kt")
//    mainClass.set("freenovekit.SoftPWMJitterTestKt")
//    mainClass.set("dork.CB1ThreadsKt")
//    mainClass.set("dork.SteppingKt")
//    mainClass.set("kobots.ops.SchwingKt")
//    mainClass.set("qwiic.oled.WithSensorKt")
    mainClass.set("device.examples.LEDShimKt")
}
