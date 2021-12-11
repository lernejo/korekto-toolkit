package com.github.lernejo.korekto.toolkit

import com.github.lernejo.korekto.toolkit.NatureFactory.Companion.lookupNatures
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer

class Exercise(val name: String, val root: Path) : AutoCloseable {
    val metadata: Metadata = MetadataParser().parse(root)
    val natures: Map<Class<out Nature<*>>, Nature<*>> = lookupNatures(this)

    @Suppress("UNCHECKED_CAST")
    fun <C : NatureContext, N : Nature<C>> lookupNature(natureClass: Class<N>): Optional<N> {
        return Optional.ofNullable(natures[natureClass] as N?)
    }

    override fun toString(): String {
        return "Exercise{" +
            "name='" + name + '\'' +
            '}'
    }

    override fun close() {
        natures.values.forEach(Consumer { obj: Nature<*> -> obj.close() })
    }

    fun writeFile(relativePath: String, content: String): Path {
        val outputFilePath = root.resolve(relativePath)
        if (!Files.exists(outputFilePath.parent)) {
            Files.createDirectories(outputFilePath.parent)
        }

        outputFilePath.toFile().writeText(content)
        return outputFilePath.toAbsolutePath()
    }
}

class Metadata(val authors: Set<Author>) {

    class Author(val name: String, val primaryEmail: String, val emails: Set<String>)

    companion object {
        private val EMPTY = Metadata(emptySet())
        fun empty(): Metadata {
            return EMPTY
        }
    }

}
