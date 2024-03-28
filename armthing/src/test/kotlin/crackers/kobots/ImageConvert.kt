/*
 * Copyright 2022-2024 by E. A. Graham, Jr.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package crackers.kobots

import java.awt.image.BufferedImage
import java.io.FilenameFilter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.io.path.exists

/**
 * Checks the download directory for material icon files and copy/converts them for use in my projects.
 */
fun main() {
    val iconDir = Paths.get(System.getProperty("user.home"), "projects/icons").also {
        if (!it.exists()) Files.createDirectories(it)
    }
    val materialDir = Paths.get(iconDir.toString(), "material").also {
        if (!it.exists()) Files.createDirectories(it)
    }

    val FILL_TAG = "_FILL0"
    val filter = FilenameFilter { _, name -> name.endsWith(".png") }
    Paths.get(System.getProperty("user.home"), "Downloads").toFile().listFiles(filter)?.forEach {
        val newFilePath: Path = it.name.let {
            if (it.contains(FILL_TAG)) {
                materialDir.resolve(it.substring(0, it.indexOf(FILL_TAG)) + ".png")
            } else {
                iconDir.resolve(it)
            }
        }

        val output = newFilePath.toFile()
        ImageIO.read(it).apply {
            println("Converting ${it.name} to ${output.name}")
            invert()
            ImageIO.write(this, "png", output)
        }
    }
}

fun BufferedImage.invert(): BufferedImage {
    for (x in 0 until width) {
        for (y in 0 until height) {
            val pixel = getRGB(x, y)
            val alpha = (pixel shr 24) and 0xFF
            val red = 255 - ((pixel shr 16) and 0xFF)
            val green = 255 - ((pixel shr 8) and 0xFF)
            val blue = 255 - (pixel and 0xFF)
            val inverted = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
            setRGB(x, y, inverted)
        }
    }
    return this
}
