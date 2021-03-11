package com.github.lernejo.korekto.toolkit.misc

import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object PropertyLoader {
    @JvmStatic
    fun loadProperties(classPath: String?): Map<String, String> {
        val resource = PropertyLoader::class.java.classLoader.getResource(classPath)
        val props: MutableMap<String, String> = LinkedHashMap()
        Files.readAllLines(Paths.get(resource.toURI())).stream()
            .filter { l: String -> !l.trim { it <= ' ' }.isBlank() }
            .forEach { l: String ->
                val parts = l.split('=', limit = 2)
                props[parts[0]] = parts[1]
            }
        return props
    }
}
