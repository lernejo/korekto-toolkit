package com.github.lernejo.korekto.toolkit.i18n

import com.github.lernejo.korekto.toolkit.misc.Loader
import org.thymeleaf.TemplateEngine
import org.thymeleaf.TemplateSpec
import org.thymeleaf.context.ExpressionContext
import org.thymeleaf.standard.StandardDialect
import org.thymeleaf.templatemode.TemplateMode
import java.util.*
import java.util.stream.Stream


class I18nTemplateResolver {
    fun process(templateName: String, context: Map<String, Any>, locale: Locale): String {
        val ic = ExpressionContext(templateEngine.configuration, locale, context)
        return templateEngine.process(TemplateSpec(loadTemplate(templateName, locale), TemplateMode.TEXT), ic)
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
            .map { I18nTemplateResolver::class.java.classLoader.getResourceAsStream("templates/${prefix}_$it$extension") }
            .filter { it != null }
            .map { inputStream -> inputStream.use { Loader.toString(it) } }
            .findFirst()
            .orElseThrow()
    }

    companion object {
        val templateEngine: TemplateEngine by lazy {
            createEngine()
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
