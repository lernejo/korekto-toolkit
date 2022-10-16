package com.github.lernejo.korekto.toolkit.thirdparty.pmd

import com.github.lernejo.korekto.toolkit.Exercise
import com.github.lernejo.korekto.toolkit.misc.writeFile
import net.sourceforge.pmd.PMD
import net.sourceforge.pmd.PMD.StatusCode
import net.sourceforge.pmd.PMDConfiguration
import net.sourceforge.pmd.cli.PmdParametersParseResult
import net.sourceforge.pmd.cli.internal.CliMessages
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors

class PmdExecutor {

    companion object {
        init {
            SLF4JBridgeHandler.removeHandlersForRootLogger()
            SLF4JBridgeHandler.install()
        }
    }

    private val logger = LoggerFactory.getLogger(PmdExecutor::class.java)

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
            logger.warn("No source directory")
            return null
        }
        val arguments = arrayOf(
            "--cache", path.resolve("target/pmd.cache").toAbsolutePath().toString(),
            "-d", srcDirectory.toAbsolutePath().toString(),
            "-f", "xml",
            "-R", ruleSetPath.toString(),
            "-r", reportPath.toString()
        )
        val exitCode = runPmd(arguments).toInt()
        if (exitCode == 1) {
            logger.warn("PMD failed with errors")
            return null
        }
        return parse(reportPath)
    }

    private fun runPmd(args: Array<String>): StatusCode {
        val parseResult = PmdParametersParseResult.extractParameters(*args)

        if (parseResult.deprecatedOptionsUsed.isNotEmpty()) {
            val (key, value) = parseResult.deprecatedOptionsUsed.entries.iterator().next()
            logger.warn("PMD: Some deprecated options were used on the command-line, including $key")
            logger.warn("PMD: Consider replacing it with $value")
        }
        if (parseResult.isError) {
            System.err.println(parseResult.error.message)
            System.err.println(CliMessages.runWithHelpFlagMessage())
            return StatusCode.ERROR
        }
        return runPmd(parseResult.toConfiguration())
    }

    @Suppress("DEPRECATION")
    private fun runPmd(configuration: PMDConfiguration): StatusCode {
        val status: StatusCode = try {
            val violations = PMD.doPMD(configuration)
            if (violations < 0) {
                StatusCode.ERROR
            } else if (violations > 0 && configuration.isFailOnViolation) {
                StatusCode.VIOLATIONS_FOUND
            } else {
                StatusCode.OK
            }
        } catch (e: Exception) {
            System.err.println(e.message)
            StatusCode.ERROR
        }
        return status
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
