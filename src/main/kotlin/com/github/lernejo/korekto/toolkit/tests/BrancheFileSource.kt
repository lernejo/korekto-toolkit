package com.github.lernejo.korekto.toolkit.tests

import com.github.lernejo.korekto.toolkit.misc.SubjectForToolkitInclusion
import com.github.lernejo.korekto.toolkit.tests.BrancheFileSource.BrancheFileSourceProvider
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

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
    BrancheFileSourceProvider::class
)
annotation class BrancheFileSource {
    class BrancheFileSourceProvider : ArgumentsProvider {
        @Throws(Exception::class)
        override fun provideArguments(extensionContext: ExtensionContext): Stream<out Arguments> {
            val branchesDirectoryUrl = BrancheFileSource::class.java.classLoader.getResource("branches")
            val branchesDirectory = Paths.get(branchesDirectoryUrl?.toURI()!!)
            return Files.list(branchesDirectory)
                .filter { path: Path -> Files.isRegularFile(path) }
                .map {buildArguments(it)}
        }

        @Throws(IOException::class)
        private fun buildArguments(p: Path): Arguments {
            val fileName = removeFileExtension(p.fileName.toString())
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
                if (filename == null || filename.isEmpty()) {
                    return filename
                }
                val extPattern = "(?<!^)[.]" + if (removeAllExtensions) ".*" else "[^.]*$"
                return filename.replace(extPattern.toRegex(), "")
            }
        }
    }
}
