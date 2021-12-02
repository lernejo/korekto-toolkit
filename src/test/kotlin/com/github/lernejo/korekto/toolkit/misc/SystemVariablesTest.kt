package com.github.lernejo.korekto.toolkit.misc

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class SystemVariablesTest {

    @Test
    internal fun var_from_props() {
        val key = "some_test_1234"
        val value = "test-1234"
        System.setProperty(key, value)

        Assertions.assertThat(SystemVariables[key]).isEqualTo(value)
    }

    @Test
    internal fun var_from_env() {
        val key = if (OS.WINDOWS.isCurrentOs) "Path" else "PATH"

        Assertions.assertThat(SystemVariables[key]).isNotEmpty
    }
}
