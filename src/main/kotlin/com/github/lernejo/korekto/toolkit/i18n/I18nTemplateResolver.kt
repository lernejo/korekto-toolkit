package com.github.lernejo.korekto.toolkit.i18n

import com.github.lernejo.korekto.toolkit.misc.Loader
import org.thymeleaf.TemplateEngine
import org.thymeleaf.TemplateSpec
import org.thymeleaf.context.ExpressionContext
import org.thymeleaf.standard.StandardDialect
import org.thymeleaf.templatemode.TemplateMode
import java.util.*
import java.util.stream.Stream

internal data class CacheKey(val templateName: String, val locale: Locale)

class I18nTemplateResolver {
    fun process(templateName: String, context: Map<String, Any>, locale: Locale): String {
        val ic = ExpressionContext(templateEngine.configuration, locale, context)
        val templateMode = if (templateName.endsWith(".xml")) TemplateMode.XML else TemplateMode.TEXT
        val template = templateCache.computeIfAbsent(CacheKey(templateName, locale)) { key ->
            loadTemplate(
                key.templateName,
                key.locale
            )
        }
        return templateEngine.process(TemplateSpec(template, templateMode), ic)
    }

    private fun loadTemplate(templateName: String, locale: Locale): String {
        val extensionStart = templateName.lastIndexOf('.')
        val (prefix, extension) = if (extensionStart > -1) {
            Pair(templateName.substring(0, extensionStart), templateName.substring(extensionStart))
        } else {
            Pair(templateName, "")
        }
        return Stream.of(locale, Locale.getDefault())
            .map { it.language }
            .map { if (it.isNotEmpty()) "_$it" else it }
            .map { I18nTemplateResolver::class.java.classLoader.getResourceAsStream("templates/${prefix}$it$extension") }
            .filter { it != null }
            .map { inputStream -> inputStream.use { Loader.toString(it) } }
            .findFirst()
            .orElseThrow()
    }

    companion object {
        val templateEngine: TemplateEngine by lazy {
            createEngine()
        }

        private val cacheMaxSize = 15

        private val templateCache = object : LinkedHashMap<CacheKey, String>() {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, String>) = size > cacheMaxSize
        }

        private fun createEngine(): TemplateEngine {
            val dialect = StandardDialect()
            dialect.conversionService = CustomStandardConversionService()

            val templateEngine = TemplateEngine()
            templateEngine.setDialect(dialect)
            return templateEngine
        }
    }
}
