package com.github.lernejo.korekto.toolkit.thirdparty.pmd

import com.github.lernejo.korekto.toolkit.Exercise
import com.github.lernejo.korekto.toolkit.misc.writeFile
import net.sourceforge.pmd.PMD
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors

class PmdExecutor {

    private val LOGGER = LoggerFactory.getLogger(PmdExecutor::class.java)

    fun runPmd(exercise: Exercise, vararg rules: Rule): List<PmdReport> {
        val ruleSet = RuleSetGenerator().generateRuleSet(*rules)

        val mavenModules = Files.walk(exercise.root)
            .filter { f -> f.toString().endsWith("src" + File.separator + "main" + File.separator + "java") }
            .filter(Files::isDirectory)
            .map { f -> f.parent.parent.parent }
            .collect(Collectors.toList())

        return mavenModules
            .map { runPmd(it, ruleSet) }
            .filter(Objects::nonNull)
            .map { r -> r!! }
    }

    private fun runPmd(path: Path, ruleSet: String): PmdReport? {
        val ruleSetPath = path.writeFile("ruleSet.xml", ruleSet)

        val reportPath = path.writeFile("target/pmd.xml", "")
        val srcDirectory = path.resolve("src/main/java")
        if (!Files.exists(srcDirectory)) {
            LOGGER.warn("No source directory")
            return null
        }
        val arguments = arrayOf(
            "--cache", path.resolve("target/pmd.cache").toAbsolutePath().toString(),
            "-d", srcDirectory.toAbsolutePath().toString(),
            "-f", "xml",
            "-R", ruleSetPath.toString(),
            "-r", reportPath.toString()
        )
        val exitCode = PMD.run(arguments)
        if (exitCode == 1) {
            LOGGER.warn("PMD failed with errors")
            return null
        }
        return parse(reportPath)
    }
}


data class PmdReport(val fileReports: List<FileReport>)
data class FileReport(val name: String, val violations: List<Violation>)
data class Violation(
    val beginLine: Int,
    val beginColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val packageName: String,
    val className: String,
    val method: String?,
    val variable: String?,
    val rule: String,
    val ruleSet: String,
    val priority: Int,
    val message: String
)
