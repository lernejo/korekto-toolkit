package com.github.lernejo.korekto.toolkit.partgrader

import com.github.lernejo.korekto.toolkit.GradePart
import com.github.lernejo.korekto.toolkit.GradingContext
import com.github.lernejo.korekto.toolkit.PartGrader
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.FileReport
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.PmdExecutor
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.PmdReportTranslator
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.Rule
import kotlin.math.abs
import kotlin.math.max

class PmdPartGrader<T : GradingContext>(
    private val name: String,
    private val minGrade: Double,
    private val pointPerViolation: Double = 1.0,
    private vararg val rules: Rule
) : PartGrader<T> {
    override fun name() = name
    override fun minGrade() = minGrade

    override fun grade(context: T): GradePart {
        val pmdReports = PmdExecutor().runPmd(
            context.exercise!!,
            *rules
        )

        val (violationCount, messages) = PmdReportTranslator()(pmdReports, rules)

        return GradePart(name(), max(-1 * violationCount * abs(pointPerViolation), minGrade()), null, messages)
    }
}
