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
        if (pmdReports.isEmpty()) {
            return GradePart(name(), 0.0, null, listOf("No analysis can be performed"))
        }

        val sortedReports = pmdReports.sortedBy { r -> r.fileReports.size }

        var violations = 0L
        val messages: MutableList<String> = mutableListOf()

        for (pmdReport in sortedReports) {
            val fileReports = pmdReport.fileReports.sortedBy { fr -> fr.name }

            violations += fileReports.stream().mapToLong { (_, violations1) -> violations1.size.toLong() }.sum()
            fileReports
                .map { fr: FileReport ->
                    fr.name + buildViolationsBlock(
                        fr
                    )
                }
                .forEach { e: String -> messages.add(e) }
        }

        if (messages.isEmpty()) {
            messages.add("OK")
        }
        return GradePart(name(), max(violations * minGrade() / 4, minGrade()), null, messages)
    }

    private fun buildViolationsBlock(fileReport: FileReport) = fileReport
        .violations
        .sortedBy { it.beginLine * 10000 + it.beginColumn }.joinToString(
            "\n            * ",
            "\n            * ",
            ""
        ) { v -> "L." + v.beginLine + ": " + v.message.trim { it <= ' ' } }
}
