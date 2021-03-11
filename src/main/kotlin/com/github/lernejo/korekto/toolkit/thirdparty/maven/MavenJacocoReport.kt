package com.github.lernejo.korekto.toolkit.thirdparty.maven

import com.github.lernejo.korekto.toolkit.Exercise
import org.xml.sax.InputSource
import java.io.StringReader
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

data class MavenJacocoReport(
    val missed: Int,
    val covered: Int,
    val tracked: Int,
    val ratio: Double
) {
    companion object {
        private val factory = DocumentBuilderFactory.newInstance()

        @JvmStatic
        fun from(exercise: Exercise): Optional<MavenJacocoReport> {
            try {
                val builder = factory.newDocumentBuilder()
                builder.setEntityResolver { publicId: String?, systemId: String ->
                    if (systemId.endsWith(".dtd")) {
                        return@setEntityResolver InputSource(StringReader(""))
                    } else {
                        return@setEntityResolver null
                    }
                }
                val root =
                    builder.parse(exercise.root.resolve("target/site/jacoco/jacoco.xml").toFile()).documentElement
                val path = XPathFactory.newInstance().newXPath()

                val missed = path.evaluate("/report/counter[@type='LINE']/@missed", root).toInt()
                val covered = path.evaluate("/report/counter[@type='LINE']/@covered", root).toInt()
                val tracked = missed + covered
                val ratio = covered.toDouble() / tracked
                return Optional.of(MavenJacocoReport(missed, covered, tracked, ratio))
            } catch (e: Exception) {
                return Optional.empty()
            }
        }
    }
}
