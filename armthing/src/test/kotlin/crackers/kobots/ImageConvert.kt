/*
 * Copyright 2022-2023 by E. A. Graham, Jr.
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
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.io.path.exists

/**
 * Checks the download directory for material icon files and copy/converts them for use in my projects.
 */
fun main() {
    val materialDir = Paths.get(System.getProperty("user.home"), "projects/icons/material").also {
        if (!it.exists()) Files.createDirectories(it)
    }

    val FILL_TAG = "_FILL0"
    Paths.get(System.getProperty("user.home"), "Downloads").toFile().listFiles()?.forEach {
        if (it.name.contains(FILL_TAG)) {
            val newFileName = it.name.let { name ->
                name.substring(0, name.indexOf(FILL_TAG)) + ".png"
            }
            val output = materialDir.resolve(newFileName).toFile()
            ImageIO.read(it).apply {
                invert()
                ImageIO.write(this, "png", output)
            }
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
