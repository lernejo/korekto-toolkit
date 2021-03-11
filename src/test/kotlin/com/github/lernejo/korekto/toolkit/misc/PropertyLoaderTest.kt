package com.github.lernejo.korekto.toolkit.misc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PropertyLoaderTest {

    @Test
    internal fun `load properties keeps order`() {
        val properties = PropertyLoader.loadProperties("sample.properties")

        assertThat(properties).containsExactlyEntriesOf(
            linkedMapOf(
                "toto" to "titi=2",
                "titi" to "tutu"
            )
        )
    }
}
