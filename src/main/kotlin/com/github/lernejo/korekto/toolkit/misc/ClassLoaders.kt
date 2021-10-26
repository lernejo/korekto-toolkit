package com.github.lernejo.korekto.toolkit.misc

import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path

object ClassLoaders {

    @JvmStatic
    fun newIsolatedClassLoader(vararg additionalPaths: Path): URLClassLoader {
        val classpath = System.getProperty("java.class.path").split(File.pathSeparator)
            .map { first: String? -> Path.of(first) }
            .map { p: Path ->
                try {
                    return@map p.toUri().toURL()
                } catch (e: MalformedURLException) {
                    throw RuntimeException(e)
                }
            }.toTypedArray()

        val isolatedClasspath = arrayOfNulls<URL>(classpath.size + additionalPaths.size)

        additionalPaths.forEachIndexed { index, path -> isolatedClasspath[index] = toUrl(path) }
        System.arraycopy(classpath, 0, isolatedClasspath, additionalPaths.size, classpath.size)

        return URLClassLoader(
            isolatedClasspath,
            ClassLoaders::class.java.classLoader.parent
        )
    }

    private fun toUrl(path: Path) = path.toUri().toURL()
}
