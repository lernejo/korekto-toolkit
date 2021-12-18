package com.github.lernejo.korekto.toolkit.misc

import java.nio.file.Files
import java.nio.file.Path

fun Path.writeFile(relativePath: String, content: String): Path {
    val outputFilePath = resolve(relativePath)
    if (!Files.exists(outputFilePath.parent)) {
        Files.createDirectories(outputFilePath.parent)
    }

    outputFilePath.toFile().writeText(content)
    return outputFilePath.toAbsolutePath()
}
