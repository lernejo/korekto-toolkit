package com.github.lernejo.korekto.toolkit.thirdparty.maven

import com.github.lernejo.korekto.toolkit.Exercise
import org.slf4j.LoggerFactory
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory
import kotlin.io.path.name

data class MavenJacocoReport(
    val missed: Int,
    val covered: Int,
    val tracked: Int,
    val ratio: Double
) {
    companion object {
        private val factory = DocumentBuilderFactory.newInstance()
        private val logger = LoggerFactory.getLogger(MavenJacocoReport::class.java)

        @JvmStatic
        fun from(exercise: Exercise): List<MavenJacocoReport> {
            val builder = factory.newDocumentBuilder()
            builder.setEntityResolver { publicId: String?, systemId: String ->
                if (systemId.endsWith(".dtd")) {
                    return@setEntityResolver InputSource(StringReader(""))
                } else {
                    return@setEntityResolver null
                }
            }
            val jacocoReportPaths = Files.walk(exercise.root)
                .filter { f -> f.toString().contains("target" + File.separator + "site") }
                .filter(Files::isRegularFile)
                .filter { f -> f.name == "jacoco.xml" }
                .collect(Collectors.toList())

            return jacocoReportPaths
                .map { parseReport(builder, it) }
                .filter(Objects::nonNull)
                .map { r -> r!! }
        }

        @JvmStatic
        fun merge(report: List<MavenJacocoReport>): MavenJacocoReport {
            val covered = report.sumOf { r -> r.covered }
            val tracked = report.sumOf { r -> r.tracked }
            val ratio = covered.toDouble() / tracked
            return MavenJacocoReport(report.sumOf { r -> r.missed }, covered, tracked, ratio)
        }

        private fun parseReport(documentBuilder: DocumentBuilder, path: Path): MavenJacocoReport? {
            return try {
                val root = documentBuilder.parse(path.toFile()).documentElement
                val path = XPathFactory.newInstance().newXPath()

                val missed = path.evaluate("/report/counter[@type='LINE']/@missed", root).toInt()
                val covered = path.evaluate("/report/counter[@type='LINE']/@covered", root).toInt()
                val tracked = missed + covered
                val ratio = covered.toDouble() / tracked
                MavenJacocoReport(missed, covered, tracked, ratio)
            } catch (e: Exception) {
                logger.warn("Failed to parse report at $path")
                null
            }
        }
    }
}
