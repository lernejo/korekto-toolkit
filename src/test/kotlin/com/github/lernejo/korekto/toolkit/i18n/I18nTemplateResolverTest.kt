package com.github.lernejo.korekto.toolkit.i18n

import com.github.lernejo.korekto.toolkit.GradePart
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

internal class I18nTemplateResolverTest {
    @Test
    internal fun `process live-issue with locale`() {
        val context = mapOf(
            "grade" to 2.3,
            "maxGrade" to 5.0,
            "gradeParts" to listOf(
                GradePart("Part 1", 0.8, 1.0, listOf("toto", "titi")),
                GradePart("Part 2", 1.4, 2.0, listOf("truc"))
            ),
            "deadline" to Instant.ofEpochSecond(1615884854).plus(3, ChronoUnit.DAYS),
            "now" to Instant.ofEpochSecond(1615884854)
        )
        val body = I18nTemplateResolver().process("live-issue/body.md", context, Locale.FRENCH)

        Assertions.assertThat(body).isEqualTo(
            """
            # Exercice commencé
            Votre note est de **2.3**/5.  
            
            ## Détail
            * Part 1: 0.8/1
                * toto
                * titi
            
            * Part 2: 1.4/2
                * truc
            
            
            
            Vous avez jusqu'à 2021-03-19T08:54:14Z pour améliorer votre note.
            
            *Analyse effectuée à 2021-03-16T08:54:14Z.*
            
        """.trimIndent()
        )
    }

    @Test
    internal fun `process gg-issue with locale`() {
        val context = mapOf(
            "grade" to 2.3,
            "maxGrade" to 5.0
        )
        val body = I18nTemplateResolver().process("gg-issue/body.md", context, Locale.FRENCH)

        Assertions.assertThat(body).isEqualTo(
            """
            # Exercice fini
            Votre note finale est de **2.3**/5.

            Bravo !
            
        """.trimIndent()
        )
    }
}
