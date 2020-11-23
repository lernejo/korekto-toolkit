package com.github.lernejo.korekto.toolkit

import kotlin.jvm.Throws

class MetadataParserV1 : VersionedMetadataParser {

    companion object {
        const val VERSION = "korekto/exercise/v1"
    }

    @Throws(MetadataParsingException::class)
    @Suppress("UNCHECKED_CAST")
    override fun parse(yaml: Map<String, Any>): Metadata {
        val authors = yaml["authors"] as List<Map<String, Any>>? ?: listOf()
        return Metadata(authors
                .map { a ->
                    Metadata.Author(
                            a["name"] as String,
                            a["primary-email"] as String,
                            emailsNodeToList(a["emails"])
                    )
                }
                .toSet())
    }

    @Suppress("UNCHECKED_CAST")
    private fun emailsNodeToList(arg: Any?): Set<String> {
        if (arg == null) {
            return setOf()
        }
        return when {
            arg is Collection<*> -> {
                (arg as Collection<String>).toSet()
            }
            (arg) is String -> {
                setOf(arg)
            }
            else -> {
                throw InternalParsingException("Invalid YAML structure for emails")
            }
        }
    }
}
