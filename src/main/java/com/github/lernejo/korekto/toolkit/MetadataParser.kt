package com.github.lernejo.korekto.toolkit

import org.yaml.snakeyaml.Yaml
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Path
import kotlin.jvm.Throws

class MetadataParser {

    private val parsersByVersion = mapOf(
            MetadataParserV1.VERSION to MetadataParserV1()
    )

    @Throws(MetadataParsingException::class)
    fun parse(exercisePath: Path): Metadata {
        val configurationPath = exercisePath.resolve(".exercise.yml").toAbsolutePath()
        try {
            FileInputStream(configurationPath.toFile()).use { `is` ->
                val yaml = Yaml()
                val document = yaml.load<Map<String, Any>>(`is`)
                val apiVersion = document["apiVersion"]
                if (apiVersion == null || apiVersion !is String) {
                    throw MetadataParsingException(configurationPath, "Missing apiVersion field from")
                }
                val parser = parsersByVersion[apiVersion]
                        ?: throw MetadataParsingException(configurationPath, "Unknown apiVersion '$apiVersion' in")

                return parser.parse(document)
            }
        } catch (e: FileNotFoundException) {
            return Metadata.empty()
        } catch (e: IOException) {
            throw MetadataParsingException(configurationPath, "Unable to read", e)
        } catch(e: InternalParsingException) {
            throw MetadataParsingException(configurationPath, e.message!!)
        }
    }
}

interface VersionedMetadataParser {

    fun parse(yaml: Map<String, Any>): Metadata
}

internal class InternalParsingException(prefix: String) : RuntimeException(prefix)

private fun formatMessage(configurationPath: Path, messagePrefix: String): String {
    return "$messagePrefix Korekto configuration file $configurationPath"
}

class MetadataParsingException : RuntimeException {
    constructor(configurationPath: Path, messagePrefix: String, cause: Exception) : super(formatMessage(configurationPath, messagePrefix), cause)

    constructor(configurationPath: Path, messagePrefix: String) : super(formatMessage(configurationPath, messagePrefix))
}
