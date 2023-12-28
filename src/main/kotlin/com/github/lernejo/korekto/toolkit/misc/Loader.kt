package com.github.lernejo.korekto.toolkit.misc

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.stream.Collectors

object Loader {
    @JvmStatic
    fun loadProperties(classPath: String?): Map<String, String> {
        val resource = Loader::class.java.classLoader.getResource(classPath)!!
        val props: MutableMap<String, String> = LinkedHashMap()
        Files.readAllLines(Paths.get(resource.toURI())).stream()
            .filter { l: String -> l.trim { it <= ' ' }.isNotBlank() }
            .forEach { l: String ->
                val parts = l.split('=', limit = 2)
                props[parts[0]] = parts[1]
            }
        return props
    }

    @JvmStatic
    fun loadLines(path: Path): List<String> {
        return Files.readAllLines(path).stream()
            .filter { l: String -> l.trim { it <= ' ' }.isNotBlank() }
            .collect(Collectors.toList())
    }

    fun toString(inputStream: InputStream): String =
        Scanner(inputStream, StandardCharsets.UTF_8).use { scanner -> scanner.useDelimiter("\\A").next() }

    fun copyPathToFile(classPath: String, targetFilePath: String) {
        val targetPath = Paths.get(targetFilePath)
        Loader::class.java.classLoader.getResourceAsStream(classPath)!!.use {
            Files.copy(it, targetPath, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
