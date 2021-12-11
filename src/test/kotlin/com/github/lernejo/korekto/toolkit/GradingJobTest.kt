package com.github.lernejo.korekto.toolkit

import com.github.lernejo.korekto.toolkit.thirdparty.git.ExerciseCloner.Companion.toGitHubHttps
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

internal class GradingJobTest {

    @Test
    internal fun grading_job_execution() {
        val holder = AtomicReference<GradeDetails>()
        val exitCode = GradingJob()
            .addCloneStep()
            .addStep("test_1") { context ->
                context.gradeDetails.parts.add(
                    GradePart(
                        "test_1",
                        3.0,
                        7.0,
                        listOf("comment1", "comment2")
                    )
                ); holder.set(context.gradeDetails)
            }
            .addSendStep()
            .run(
                GradingConfiguration(
                    repoUrl = toGitHubHttps("lernejo/korekto-toolkit"),
                    callbackUrl = "http://localhost:80/service"
                )
            )

        assertThat(holder.get().parts).hasSize(1)
        assertThat(exitCode).isEqualTo(1)
    }
}
