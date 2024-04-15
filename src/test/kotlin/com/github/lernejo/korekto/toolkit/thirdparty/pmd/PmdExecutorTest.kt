package com.github.lernejo.korekto.toolkit.thirdparty.pmd

import com.github.lernejo.korekto.toolkit.Exercise
import com.github.lernejo.korekto.toolkit.thirdparty.git.ExerciseCloner
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.Rule.Companion.buildEmptyControlStatementRule
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.Rule.Companion.buildExcessiveClassLengthRule
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.Rule.Companion.buildExcessiveMethodLengthRule
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.Rule.Companion.buildFieldMandatoryModifierRule
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.Rule.Companion.buildMethodNamingConventionsRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Paths
import java.util.stream.Stream

internal class PmdExecutorTest {
    @Test
    internal fun pmd_process_rules() {
        val exercise =
            ExerciseCloner(Paths.get("target/github")).gitClone(
                ExerciseCloner.toGitHubHttps("fridujo/classpath-junit-extension")
            )
        val rules = arrayOf(
            buildExcessiveClassLengthRule(100, 0),
            buildExcessiveMethodLengthRule(12, 0),
            buildFieldMandatoryModifierRule(0, "private", "final", "!static")
        )
        val reports = PmdExecutor().runPmd(
            exercise,
            *rules
        )
        val (_, messages) = PmdReportTranslator()(reports, rules)

        messages.forEach(System.out::println)

        assertThat(messages).hasSize(9)
    }

    data class TestViolation(val file: String, val line: Int, val message: String)

    @ParameterizedTest
    @MethodSource("rule_cases")
    fun check_rule(rule: Rule, violations: Set<TestViolation>) {
        val exercise = Exercise(
            "test",
            Paths.get(PmdExecutorTest::class.java.classLoader.getResource("fake_exercises/pmd_checker").toURI())
        )

        val report = PmdExecutor().runPmd(
            exercise,
            rule
        )
        val actualViolations = report[0].fileReports
            .flatMap { r -> r.violations.map { v -> TestViolation(r.name, v.beginLine, v.message) } }
            .toSet()

        assertThat(actualViolations).containsAll(violations)
    }

    companion object {
        @JvmStatic
        fun rule_cases(): Stream<Arguments> = Stream.of(
            Arguments.of(
                buildMethodNamingConventionsRule(),
                setOf(
                    TestViolation(
                        "sample.Sample",
                        11,
                        "Method name should follow lowerCamelCase convention, but `Badly_Named_METHOD` found instead"
                    )
                )
            ),
            Arguments.of(
                buildExcessiveMethodLengthRule(1, 0),
                setOf(
                    TestViolation(
                        "sample.Sample",
                        5,
                        "method stuff() has exceeding the maximum of 1 lines (recorded 2 lines)"
                    ),
                    TestViolation(
                        "sample.Sample",
                        15,
                        "method wellNamedMethod(String) has exceeding the maximum of 1 lines (recorded 2 lines)"
                    )
                )
            ),
            Arguments.of(
                buildExcessiveClassLengthRule(5, 1),
                setOf(
                    TestViolation(
                        "sample.Sample",
                        3,
                        "class Sample has exceeding the maximum of 5 lines (recorded 16 lines)"
                    ),
                )
            ),
            Arguments.of(
                buildEmptyControlStatementRule(),
                setOf(TestViolation("sample.Sample", 6, "Empty if statement"))
            ),
        )
    }
}
