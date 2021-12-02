package com.github.lernejo.korekto.toolkit.thirdparty.maven

import com.github.lernejo.korekto.toolkit.misc.Processes.launch
import com.github.lernejo.korekto.toolkit.misc.SystemVariables
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern


object MavenResolver {

    private val MAVEN_HOME_LINE_PATTERN =
        Pattern.compile("^" + Pattern.quote("Maven home: ").toString() + "(?<home>.+)$", Pattern.MULTILINE)

    const val M2_HOME = "M2_HOME"
    const val MAVEN_HOME = "MAVEN_HOME"

    @JvmStatic
    fun declareMavenHomeIfNeeded() {
        val mavenHome = getMavenHome()
        if (mavenHome != null && System.getProperty("maven.home") == null) {
            System.setProperty("maven.home", mavenHome!!.toString())
        }
    }

    @JvmStatic
    fun getMavenHome(): Path? {
        val rawMavenHomeFromSystemVars = SystemVariables[MAVEN_HOME] ?: SystemVariables[M2_HOME]
        val mavenHomeFromSysVars: Path? = rawMavenHomeFromSystemVars
            ?.let { Paths.get(it) }?.takeIf { Files.exists(it) }.takeIf { Files.isDirectory(it) }

        return mavenHomeFromSysVars ?: getMavenHomeFromCli()
    }

    private fun getMavenHomeFromCli(): Path? {
        val result = launch("mvn -B -version")
        if (result.exitCode != 0) {
            return null
        }
        val matcher = MAVEN_HOME_LINE_PATTERN.matcher(result.output)
        return if (matcher.find()) {
            val homeStr = matcher.group("home")
            Paths.get(homeStr).normalize()
        } else {
            null
        }
    }
}

data class MavenInfo(val mavenHome: Path, val localRepository: Path)
