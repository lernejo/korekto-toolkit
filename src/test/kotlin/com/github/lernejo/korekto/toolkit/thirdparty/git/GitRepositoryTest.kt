package com.github.lernejo.korekto.toolkit.thirdparty.git

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class GitRepositoryTest {

    @Test
    internal fun extractCredParts() {
        val (uriWithoutCred, username, password) = GitRepository.extractCredParts("https://x-access-token:my.token@github.com/owner/repo.git")

        assertThat(uriWithoutCred).isEqualTo("https://github.com/owner/repo.git")
        assertThat(username).isEqualTo("x-access-token")
        assertThat(password).isEqualTo("my.token")
    }
}
