package com.github.lernejo.korekto.toolkit.thirdparty.pmd

import com.github.lernejo.korekto.toolkit.thirdparty.git.ExerciseCloner
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules.ExcessiveClassLengthRule
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules.ExcessiveMethodLengthRule
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules.FieldMandatoryModifiersRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import java.util.Map

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
}
