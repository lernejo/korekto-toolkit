package com.github.lernejo.korekto.toolkit.thirdparty.maven

import com.github.lernejo.korekto.toolkit.Exercise
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.codehaus.plexus.util.xml.pull.XmlPullParserException
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path

object MavenReader {

    @JvmStatic
    fun readModel(exercise: Exercise): Model {
        val pomFilePath = pomFilePath(exercise)
        return readModel(pomFilePath)
    }

    fun readModel(pomFilePath: Path): Model {
        val reader = MavenXpp3Reader()
        val model: Model
        try {
            Files.newInputStream(pomFilePath)
                .use { `is` -> model = reader.read(`is`) }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        } catch (e: XmlPullParserException) {
            throw RuntimeException(e)
        }
        return model
    }

    fun pomFilePath(exercise: Exercise): Path {
        return exercise.root.resolve("pom.xml")
    }
}
