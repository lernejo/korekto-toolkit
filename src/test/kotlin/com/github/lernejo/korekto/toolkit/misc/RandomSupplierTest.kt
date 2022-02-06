package com.github.lernejo.korekto.toolkit.misc

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.RepeatedTest

class RandomSupplierTest {

    @RepeatedTest(2)
    internal fun `not so random UUID gen`() {
        val rs = RandomSupplier.createDeterministic()

        val softly = SoftAssertions()
        softly.assertThat(rs.nextUuid().toString()).isEqualTo("81828384-8586-4788-898a-8b8c8d8e8f90")
        softly.assertThat(rs.nextUuid().toString()).isEqualTo("91929394-9596-4798-999a-9b9c9d9e9fa0")
        softly.assertThat(rs.nextUuid().toString()).isEqualTo("a1a2a3a4-a5a6-47a8-a9aa-abacadaeafb0")
        softly.assertThat(rs.nextUuid().toString()).isEqualTo("b1b2b3b4-b5b6-47b8-b9ba-bbbcbdbebfc0")
        softly.assertAll()
    }
}
