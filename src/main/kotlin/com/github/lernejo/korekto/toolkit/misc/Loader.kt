package com.github.lernejo.korekto.toolkit.misc

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object Loader {
    @JvmStatic
    fun loadProperties(classPath: String?): Map<String, String> {
        val resource = Loader::class.java.classLoader.getResource(classPath)
        val props: MutableMap<String, String> = LinkedHashMap()
        Files.readAllLines(Paths.get(resource.toURI())).stream()
            .filter { l: String -> !l.trim { it <= ' ' }.isBlank() }
            .forEach { l: String ->
                val parts = l.split('=', limit = 2)
                props[parts[0]] = parts[1]
            }
        return props
    }

    fun toString(inputStream: InputStream) = Scanner(inputStream, StandardCharsets.UTF_8).use { scanner -> scanner.useDelimiter("\\A").next() }
}
