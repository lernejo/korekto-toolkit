package com.github.lernejo.korekto.toolkit.partgrader

import com.github.lernejo.korekto.toolkit.GradePart
import com.github.lernejo.korekto.toolkit.GradingContext
import com.github.lernejo.korekto.toolkit.PartGrader
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.FileReport
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.PmdExecutor
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.Rule
import kotlin.math.max

class PmdPartGrader<T : GradingContext>(
    private val name: String,
    private val minGrade: Double,
    private vararg val rules: Rule
) : PartGrader<T> {
    override fun name() = name
    override fun minGrade() = minGrade

    override fun grade(context: T): GradePart {
        val pmdReports = PmdExecutor().runPmd(
            context.exercise!!,
            *rules
        )
        val ruleTolerances = HashMap<String, Int>()
        rules.filter { it.exceptions > 0 }.forEach { ruleTolerances[it.name!!] = it.exceptions }

        if (pmdReports.isEmpty()) {
            return GradePart(name(), 0.0, null, listOf("No analysis can be performed"))
        }

        val sortedReports = pmdReports.sortedBy { r -> r.fileReports.size }

        var violations = 0L
        val messages: MutableList<String> = mutableListOf()

        for (pmdReport in sortedReports) {
            val fileReports = pmdReport.fileReports.sortedBy { fr -> fr.name }

            fileReports
                .map { fr: FileReport ->
                    buildViolationsBlock(
                        ruleTolerances,
                        fr
                    )
                }
                .filter { it.violations > 0 }
                .map {
                    violations += it.violations
                    it.fileReportName + it.report
                }
                .forEach { e -> messages.add(e) }
        }

        if (messages.isEmpty()) {
            messages.add("OK")
        }
        return GradePart(name(), max(violations * minGrade() / 4, minGrade()), null, messages)
    }

    data class ViolationBlock(val fileReportName: String, val report: String, val violations: Int)

    private fun buildViolationsBlock(ruleTolerances: HashMap<String, Int>, fileReport: FileReport): ViolationBlock {
        val violations = fileReport
            .violations
            .filter {
                if (ruleTolerances.containsKey(it.rule)) {
                    ruleTolerances.compute(it.rule) { _: String, v: Int? -> v!! - 1 }
                }
                !ruleTolerances.containsKey(it.rule) || ruleTolerances[it.rule]!! > 0
            }
        val report = violations
            .sortedBy { it.beginLine * 10000 + it.beginColumn }.joinToString(
                "\n            * ",
                "\n            * ",
                ""
            ) { v -> "L." + v.beginLine + ": " + v.message.trim { it <= ' ' } }
        return ViolationBlock(fileReport.name, report, violations.size)
    }
}
