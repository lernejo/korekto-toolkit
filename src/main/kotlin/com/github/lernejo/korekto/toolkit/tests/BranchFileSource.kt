package com.github.lernejo.korekto.toolkit.tests

import com.github.lernejo.korekto.toolkit.misc.SubjectForToolkitInclusion
import com.github.lernejo.korekto.toolkit.tests.BranchFileSource.BranchFileSourceProvider
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.streams.asStream

@SubjectForToolkitInclusion
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ArgumentsSource(
    BranchFileSourceProvider::class
)
annotation class BranchFileSource {
    class BranchFileSourceProvider : ArgumentsProvider {
        @Throws(Exception::class)
        override fun provideArguments(extensionContext: ExtensionContext): Stream<out Arguments> {
            val branchesDirectoryUrl = BranchFileSource::class.java.classLoader.getResource("branches")
            val branchesDirectory = Paths.get(branchesDirectoryUrl?.toURI()!!)
            return branchesDirectory.toFile().walk()
                .filter { it.isFile && it.path.endsWith(".md") }
                .map { buildArguments(branchesDirectory, it.toPath()) }
                .asStream()
        }

        @Throws(IOException::class)
        private fun buildArguments(root: Path, p: Path): Arguments {
            val fileName = removeFileExtension(root.relativize(p).toString().replace('\\', '/', false))
            val hyphenIndex = fileName!!.indexOf('-')
            val title: String
            val branchName: String?
            if (hyphenIndex == -1) {
                branchName = fileName
                title = "missing -title in filename"
            } else {
                branchName = fileName.substring(0, hyphenIndex)
                title = fileName.substring(hyphenIndex + 1).replace('_', ' ')
            }
            return Arguments.arguments(title, branchName, Files.readString(p).trim { it <= ' ' })
        }

        companion object {
            private fun removeFileExtension(filename: String?, removeAllExtensions: Boolean = true): String? {
                if (filename.isNullOrEmpty()) {
                    return filename
                }
                val extPattern = "(?<!^)[.]" + if (removeAllExtensions) ".*" else "[^.]*$"
                return filename.replace(extPattern.toRegex(), "")
            }
        }
    }
}
