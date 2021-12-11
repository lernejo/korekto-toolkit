package com.github.lernejo.korekto.toolkit.thirdparty.github

import com.github.lernejo.korekto.toolkit.thirdparty.git.ExerciseCloner
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.Paths

internal class GitHubNatureTest {

    @Test
    internal fun `GitHub nature is loaded`() {
        val exercise =
            ExerciseCloner(Paths.get("target/github")).gitClone(
                ExerciseCloner.toGitHubHttps("lernejo/korekto-toolkit")
            )

        val lookedUpNature = exercise.lookupNature(GitHubNature::class.java)

        Assertions.assertThat(lookedUpNature).isPresent
        val gitHubNature = lookedUpNature.get()
        Assertions.assertThat(gitHubNature.listActionRuns()).isNotEmpty
    }
}
