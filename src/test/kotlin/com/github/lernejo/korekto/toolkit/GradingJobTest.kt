package com.github.lernejo.korekto.toolkit

import com.github.lernejo.korekto.toolkit.thirdparty.git.ExerciseCloner.Companion.toGitHubHttps
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

internal class GradingJobTest {

    @Test
    internal fun grading_job_execution() {
        val counter = AtomicInteger()
        val exitCode = GradingJob()
            .addCloneStep()
            .addStep("test_1", { conf, context -> counter.set(11) })
            .addSendStep()
            .run(GradingConfiguration(repoUrl = toGitHubHttps("lernejo/korekto-toolkit"), callbackUrl = "http://localhost:80/service"))

        assertThat(counter.get()).isEqualTo(11)
        assertThat(exitCode).isEqualTo(1);
    }
}
