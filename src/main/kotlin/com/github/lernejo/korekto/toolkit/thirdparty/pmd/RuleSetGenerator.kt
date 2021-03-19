package com.github.lernejo.korekto.toolkit.thirdparty.pmd

import com.github.lernejo.korekto.toolkit.i18n.I18nTemplateResolver
import java.util.*

class RuleSetGenerator {

    fun generateRuleSet(vararg rules: Rule) =
        I18nTemplateResolver().process("pmd_rule_set/ruleSet.xml", mapOf("rules" to rules), Locale.ROOT)
}

data class Rule private constructor(
    val ref: String?,
    val clazz: String?,
    val name: String?,
    val message: String?,
    val configuration: Map<String, Any> = mapOf()
) {

    @JvmOverloads
    constructor(ref: String, message: String? = null, configuration: Map<String, Any> = mapOf()) : this(
        ref,
        null,
        null,
        message,
        configuration
    )

    @JvmOverloads
    constructor(clazz: Class<out Any>, message: String? = null, configuration: Map<String, Any> = mapOf()) : this(
        null,
        clazz.name,
        clazz.simpleName,
        message,
        configuration
    )
}
