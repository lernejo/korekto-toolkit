package com.github.lernejo.korekto.toolkit

import com.github.lernejo.korekto.toolkit.thirdparty.git.ExerciseCloner
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitNature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Paths

internal class ExerciseTest {

    @Test
    internal fun `Git nature is loaded`() {
        val exercise = ExerciseCloner(Paths.get("target/github")).gitClone(ExerciseCloner.toGitHubHttps("lernejo/korekto-toolkit"))

        val lookedUpNature = exercise.lookupNature(GitNature::class.java)

        assertThat(lookedUpNature).isPresent
        assertThat(lookedUpNature.get().withContext { it.branchNames }).contains("main")
    }
}
