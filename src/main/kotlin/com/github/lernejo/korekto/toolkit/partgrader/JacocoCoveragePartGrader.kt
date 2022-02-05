package com.github.lernejo.korekto.toolkit.partgrader

import com.github.lernejo.korekto.toolkit.GradePart
import com.github.lernejo.korekto.toolkit.GradingContext
import com.github.lernejo.korekto.toolkit.PartGrader
import com.github.lernejo.korekto.toolkit.misc.Maths.round
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenJacocoReport

class JacocoCoveragePartGrader<T>(
    private val name: String,
    private val maxGrade: Double,
    private val coverageExpectedRatio: Double
) : PartGrader<T> where T : GradingContext, T : MavenContext {

    override fun name() = name
    override fun maxGrade() = maxGrade

    override fun grade(context: T): GradePart {
        if (context.hasTestFailed()) {
            return result(listOf("Coverage not available when there is test failures"), 0.0)
        }
        val jacocoReports = MavenJacocoReport.from(context.exercise!!)

        return if (jacocoReports.isEmpty()) {
            result(listOf("No JaCoCo report produced after `mvn verify`, check tests and plugins"), 0.0)
        } else {
            val ratio = MavenJacocoReport.merge(jacocoReports).ratio
            val toleranceRatio = coverageExpectedRatio - 0.05
            if (ratio < toleranceRatio) {
                val grade = round(ratio * maxGrade() / toleranceRatio, 2)
                result(
                    listOf(
                        "Code coverage: ${round(ratio * 100, 2)}%, expected: > ${
                            round(
                                coverageExpectedRatio * 100,
                                0
                            )
                        }% with `mvn verify`"
                    ),
                    grade
                )
            } else {
                result(listOf(), maxGrade())
            }
        }
    }
}
