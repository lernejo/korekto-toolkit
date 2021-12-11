package com.github.lernejo.korekto.toolkit.thirdparty.maven

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class MavenResolverTest {

    @Test
    internal fun MAVEN_HOME_is_resolved_and_set() {
        MavenResolver.declareMavenHomeIfNeeded()

        Assertions.assertThat(System.getProperty("maven.home")).isNotEmpty
    }
}
