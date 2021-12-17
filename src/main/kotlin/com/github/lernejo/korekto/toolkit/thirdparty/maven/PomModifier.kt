package com.github.lernejo.korekto.toolkit.thirdparty.maven

import com.github.lernejo.korekto.toolkit.Exercise
import org.apache.maven.model.Dependency
import org.apache.maven.model.Model
import org.apache.maven.model.Repository
import org.apache.maven.model.io.xpp3.MavenXpp3Writer
import java.nio.file.Files
import java.nio.file.Path

object PomModifier {

    @JvmStatic
    fun addDependency(pomFilePath: Path, groupId: String, artifactId: String, version: String) {
        val model: Model = MavenReader.readModel(pomFilePath)

        val alreadySet = model.dependencies.any { d -> d.groupId == groupId && d.artifactId == artifactId }

        if (!alreadySet) {
            model.dependencies.add(Dependency().also {
                it.groupId = groupId
                it.artifactId = artifactId
                it.version = version
            })
            writeModel(pomFilePath, model)
        }
    }

    @JvmStatic
    fun addDependency(exercise: Exercise, groupId: String, artifactId: String, version: String) {
        val pomFilePath = MavenReader.pomFilePath(exercise)
        addDependency(pomFilePath, groupId, artifactId, version)
    }

    @JvmStatic
    fun addRepository(pomFilePath: Path, id: String, url: String) {
        val model: Model = MavenReader.readModel(pomFilePath)

        val alreadySet = model.repositories.any { r -> r.url == url }

        if (!alreadySet) {
            model.repositories.add(Repository().also {
                it.id = id
                it.url = url
            })
            writeModel(pomFilePath, model)
        }
    }

    @JvmStatic
    fun addRepository(exercise: Exercise, id: String, url: String) {
        val pomFilePath = MavenReader.pomFilePath(exercise)
        addRepository(pomFilePath, id, url)
    }

    private fun writeModel(pomFilePath: Path, model: Model) {
        val writer = MavenXpp3Writer()

        Files.newOutputStream(pomFilePath)
            .use { os -> writer.write(os, model) }
    }
}
