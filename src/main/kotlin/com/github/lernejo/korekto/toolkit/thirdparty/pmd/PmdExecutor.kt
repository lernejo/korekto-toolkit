package com.github.lernejo.korekto.toolkit.thirdparty.pmd

import com.github.lernejo.korekto.toolkit.Exercise
import net.sourceforge.pmd.PMD
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.util.*

class PmdExecutor {

    private val LOGGER = LoggerFactory.getLogger(PmdExecutor::class.java)

    fun runPmd(exercise: Exercise, vararg rules: Rule): Optional<PmdReport> {
        val ruleSet = RuleSetGenerator().generateRuleSet(*rules)
        val ruleSetPath = exercise.writeFile("ruleSet.xml", ruleSet)

        val reportPath = exercise.writeFile("target/pmd.xml", "")
        val srcDirectory = exercise.root.resolve("src/main/java")
        if (!Files.exists(srcDirectory)) {
            LOGGER.warn("No source directory")
            return Optional.empty()
        }
        val arguments = arrayOf(
            "-cache", exercise.root.resolve("target/pmd.cache").toAbsolutePath().toString(),
            "-d", srcDirectory.toAbsolutePath().toString(),
            "-f", "xml",
            "-R", ruleSetPath.toString(),
            "-r", reportPath.toString()
        )
        val exitCode = PMD.run(arguments)
        if (exitCode == 1) {
            LOGGER.warn("PMD failed with errors")
            return Optional.empty()
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
