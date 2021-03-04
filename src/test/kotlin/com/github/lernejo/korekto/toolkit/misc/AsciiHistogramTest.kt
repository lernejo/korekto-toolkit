package com.github.lernejo.korekto.toolkit.misc

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

internal class AsciiHistogramTest {

    @Test
    internal fun `basic histogram`() {
        val ascii = AsciiHistogram().asciiHistogram(listOf(0.0, 0.1, 4.0, 4.0, 4.0, 2.0))
        Assertions.assertThat(ascii).isEqualTo(
            """
             50% │         3
             45% │         ┬
             40% │         ║
             35% │ 2       ║
             30% │ ┬       ║
             25% │ ║       ║
             20% │ ║   1   ║
             15% │ ║   ┬   ║
             10% │ ║   ║   ║
              5% │ ║   ║   ║
              0% │ ║ 0 ║ 0 ║
                 └──────────
                   0 1 2 3 4
        """.trimIndent()
        )
    }

    @RepeatedTest(10)
    internal fun `samples from gaussian distribution`() {
        val intRange = 1..400
        val r = java.util.Random()
        val factor = 10.0 / 4
        val grades = intRange.map { r.nextGaussian() * factor + factor }.toList()
        val ascii = AsciiHistogram().asciiHistogram(grades)
        println(ascii)
    }
}
