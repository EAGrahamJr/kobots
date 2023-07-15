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

package dork.converter

import crackers.kobots.utilities.loadImage
import java.io.File
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.io.path.nameWithoutExtension

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

/**
 * TODO fill this in
 */

fun main() {
    listOf("fan").forEach {
        val name = "/$it.png"
        val originalPath = Paths.get(object {}::class.java.getResource(name)!!.toURI())
        val cloneFile = originalPath.nameWithoutExtension + "2.png"
        loadImage(name).apply {
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
            ImageIO.write(this, "png", File(cloneFile))
        }
    }
}
