package com.github.lernejo.korekto.toolkit.thirdparty.pmd

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement
import jakarta.xml.bind.annotation.XmlValue
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.stream.Collectors

internal fun parse(reportPath: Path): Optional<PmdReport> {
    return try {
        val fixedReportPath = reportPath.parent.resolve("pmdFixed.xml")
        Files.write(fixedReportPath, ByteArray(0))
        Files.lines(reportPath)
            .map { s: String ->
                s // remove namespace to avoid XSD interfering
                    .replace("xmlns=\"([^\"]+\")".toRegex(), "")
            }
            .forEach { l: String ->
                try {
                    Files.write(
                        fixedReportPath,
                        Arrays.asList(l),
                        StandardOpenOption.APPEND
                    )
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
        val jaxbContext: JAXBContext = JAXBContext.newInstance(InternalPmdReport::class.java)
        val internalPmdReport =
            jaxbContext.createUnmarshaller().unmarshal(fixedReportPath.toFile()) as InternalPmdReport
        if (internalPmdReport.files != null) {
            Optional.of(internalPmdReport.asPublicReport())
        } else {
            Optional.empty()
        }
    } catch (   // | FileNotFoundException | SAXException | ParserConfigurationException
        e: JAXBException
    ) {
        throw RuntimeException(e)
    } catch (e: IOException) {
        throw RuntimeException(e)
    }
}

@XmlRootElement(name = "pmd")
internal class InternalPmdReport(
    @field:XmlElement(name = "file")
    val files: List<InternalFileReport>
) {
    // used by Jaxb
    constructor() : this(mutableListOf())

    fun asPublicReport(): PmdReport {
        return PmdReport(files!!.stream().map(InternalFileReport::asPublicReport).collect(Collectors.toList()))
    }
}

@XmlRootElement(name = "file")
internal class InternalFileReport(
    @field:XmlAttribute
    val name: String?,
    @field:XmlElement(name = "violation")
    val violations: List<InternalViolationReport>
) {
    // used by Jaxb
    constructor() : this(null, mutableListOf())

    fun asPublicReport(): FileReport {
        return FileReport(
            computeName()!!,
            violations!!.stream().map(InternalViolationReport::asPublicReport).collect(Collectors.toList())
        )
    }

    private fun computeName(): String? {
        return violations!!.stream()
            .map { v: InternalViolationReport -> v.packageName + '.' + v.className }
            .findFirst().orElse(name)
    }
}

@XmlRootElement(name = "violation")
internal class InternalViolationReport(
    @XmlAttribute
    val beginline: Int,

    @XmlAttribute
    val begincolumn: Int,

    @XmlAttribute
    val endline: Int,

    @XmlAttribute
    val endcolumn: Int,

    @XmlAttribute(name = "package")
    val packageName: String?,

    @XmlAttribute(name = "class")
    val className: String?,

    @XmlAttribute
    val method: String?,

    @XmlAttribute
    val variable: String?,

    @XmlAttribute
    val rule: String?,

    @XmlAttribute
    val ruleset: String?,

    @XmlAttribute
    val priority: Int,

    @XmlValue
    val message: String?,
) {

    // used by Jaxb
    constructor() : this(-1, -1, -1, -1, null, null, null, null, null, null, -1, null)

    fun asPublicReport(): Violation {
        return Violation(
            beginline,
            begincolumn,
            endline,
            endcolumn,
            packageName!!,
            className!!,
            method,
            variable,
            rule!!,
            ruleset!!,
            priority,
            message!!.trim()
        )
    }
}
