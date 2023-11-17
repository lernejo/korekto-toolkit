package com.github.lernejo.korekto.toolkit.thirdparty.maven

import com.github.lernejo.korekto.toolkit.Exercise
import com.github.lernejo.korekto.toolkit.Nature
import com.github.lernejo.korekto.toolkit.NatureContext
import com.github.lernejo.korekto.toolkit.NatureFactory
import com.github.lernejo.korekto.toolkit.misc.Processes
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import kotlin.io.path.exists

private val logger = LoggerFactory.getLogger(MavenNature::class.java)

class MavenNature : Nature<MavenContext> {
    override fun <RESULT> withContext(action: (MavenContext) -> RESULT): RESULT {
        TODO("Not yet implemented")
    }
}

class MavenContext : NatureContext

class MavenNatureFactory : NatureFactory {
    override fun getNature(exercise: Exercise): Optional<Nature<*>> {
        return try {
            if (exercise.root.resolve("pom.xml").exists()) {
                MavenResolver.declareMavenHomeIfNeeded();
                val mavenHome = MavenResolver.getMavenHome()
                if (mavenHome == null) {
                    logger.warn("Fail to resolve Maven: (unable to resolve maven home)")
                }
                val processResult = Processes.launch("${mavenHome}/bin/mvn -version")
                if (processResult.exitCode == 0) {
                    logger.debug("Using: " + processResult.output)
                    Optional.of(MavenNature())
                } else {
                    logger.warn("Fail to call mvn command: " + processResult.output)
                    Optional.empty()
                }
            } else {
                logger.debug("Not a Maven project (pom.xml not found)")
                Optional.empty()
            }
        } catch (e: IOException) {
            logger.warn("Fail to resolve Maven: " + e.message)
            Optional.empty()
        }
    }
}
