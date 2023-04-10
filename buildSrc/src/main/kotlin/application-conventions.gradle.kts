plugins {
    id("kobots-hardware-conventions")
    id("com.github.johnrengelman.shadow")
    application
}

dependencies {
    implementation(project(":utilities"))
}

tasks {
    /**
     * Build a "shadow jar" for single-runnable deployment
     */
    shadowJar {
        archiveBaseName.set(project.ext.get("jar.name").toString())
        archiveVersion.set("")
        archiveClassifier.set("")
        // this is important for sing the remote client at the same time as other providers
        mergeServiceFiles()
    }

    /**
     * Deploy said shadow-jar to a remote Pi for runtime fun
     */
    create("deployApp") {
        dependsOn(":${project.name}:shadowJar")
        doLast {
            val sshTarget = System.getProperty("remote", "marvin.local")
            val name = project.ext.get("jar.name").toString()

            println("Sending $name to $sshTarget")
            exec {
                commandLine(
                    "sh", "-c", """
                scp build/libs/$name.jar $sshTarget:/home/crackers
                scp *.sh $sshTarget:/home/crackers
                """.trimIndent()
                )
            }
        }
    }
}
