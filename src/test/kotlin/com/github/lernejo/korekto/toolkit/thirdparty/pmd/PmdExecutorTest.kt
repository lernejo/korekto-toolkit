package com.github.lernejo.korekto.toolkit.thirdparty.pmd

import com.github.lernejo.korekto.toolkit.Exercise
import com.github.lernejo.korekto.toolkit.thirdparty.git.ExerciseCloner
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.Rule.Companion.buildEmptyControlStatementRule
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.Rule.Companion.buildExcessiveMethodLengthRule
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.Rule.Companion.buildMethodNamingConventionsRule
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules.ExcessiveClassLengthRule
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules.ExcessiveMethodLengthRule
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules.FieldMandatoryModifiersRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Paths
import java.util.Map
import java.util.stream.Stream

internal class PmdExecutorTest {
    @Test
    internal fun pmd_process_rules() {
        val exercise =
            ExerciseCloner(Paths.get("target/github")).gitClone(
                ExerciseCloner.toGitHubHttps("fridujo/classpath-junit-extension")
            )

        val report = PmdExecutor().runPmd(
            exercise,
            Rule(
                ExcessiveClassLengthRule::class.java,
                "Class has {0} lines, exceeding the maximum of 100",
                mapOf("minimum" to 100)
            ),
            Rule(
                ExcessiveMethodLengthRule::class.java,
                "Method has {0} lines, exceeding the maximum of 12",
                mapOf("minimum" to 12)
            ),
            Rule(FieldMandatoryModifiersRule::class.java, null, Map.of("modifiers", "private, final, !static"))
        )
        assertThat(report[0].fileReports).hasSize(14)
    }

    data class TestViolation(val file: String, val line: Int, val message: String)

    @ParameterizedTest
    @MethodSource("rule_cases")
    fun check_rule(rule: Rule, violations: Set<TestViolation>) {
        val exercise = Exercise("test", Paths.get(PmdExecutorTest::class.java.classLoader.getResource("fake_exercises/pmd_checker").toURI()))

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
            Arguments.of(buildMethodNamingConventionsRule(),
                setOf(TestViolation("sample.Sample", 11, "Method name should follow lowerCamelCase convention, but `Badly_Named_METHOD` found instead"))),
            Arguments.of(buildExcessiveMethodLengthRule(1, 0),
                setOf(
                    TestViolation("sample.Sample", 5, "Method has 4 lines, exceeding the maximum of 1"),
                    TestViolation("sample.Sample", 11, "Method has 2 lines, exceeding the maximum of 1")
                )),
            Arguments.of(buildEmptyControlStatementRule(),
                setOf(TestViolation("sample.Sample", 6, "Empty if statement"))),
        )
    }
}
