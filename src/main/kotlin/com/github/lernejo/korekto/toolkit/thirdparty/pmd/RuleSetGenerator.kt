package com.github.lernejo.korekto.toolkit.thirdparty.pmd

import com.github.lernejo.korekto.toolkit.i18n.I18nTemplateResolver
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules.ExcessiveClassLengthRule
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules.ExcessiveMethodLengthRule
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules.FieldMandatoryModifiersRule
import net.sourceforge.pmd.lang.java.rule.bestpractices.LooseCouplingRule
import net.sourceforge.pmd.lang.java.rule.bestpractices.UnusedLocalVariableRule
import net.sourceforge.pmd.lang.java.rule.bestpractices.UnusedPrivateFieldRule
import net.sourceforge.pmd.lang.java.rule.bestpractices.UnusedPrivateMethodRule
import net.sourceforge.pmd.lang.java.rule.codestyle.ClassNamingConventionsRule
import net.sourceforge.pmd.lang.java.rule.codestyle.EmptyControlStatementRule
import net.sourceforge.pmd.lang.java.rule.codestyle.MethodNamingConventionsRule
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
    val configuration: Map<String, Any> = mapOf(),
    val exceptions: Int = 0
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
    constructor(clazz: Class<out Any>, message: String? = null, configuration: Map<String, Any> = mapOf(), exceptions: Int = 0) : this(
        null,
        clazz.name,
        clazz.simpleName,
        message,
        configuration,
        exceptions
    )

    companion object {
        @JvmStatic
        @JvmOverloads
        fun buildExcessiveClassLengthRule(max: Int, toleranceMargin: Int = 2) = Rule(
            ExcessiveClassLengthRule::class.java,
            "Class has {0} lines, exceeding the maximum of $max",
            mapOf("minimum" to max + toleranceMargin)
        )

        @JvmStatic
        @JvmOverloads
        fun buildExcessiveMethodLengthRule(max: Int, toleranceMargin: Int = 2) = Rule(
            ExcessiveMethodLengthRule::class.java,
            "Method has {0} lines, exceeding the maximum of $max",
            mapOf("minimum" to max + toleranceMargin)
        )

        @JvmStatic
        @JvmOverloads
        fun buildFieldMandatoryModifierRule(exceptions: Int = 0, vararg modifiers: String = arrayOf("private", "final")) = Rule(
            FieldMandatoryModifiersRule::class.java,
            null,
            mapOf("modifiers" to modifiers.joinToString(", ")),
            exceptions
        )

        @JvmStatic
        @JvmOverloads
        fun buildDependencyInversionRule() = Rule(
            LooseCouplingRule::class.java,
            "Dependency inversion principle not respected: type `{0}` should be replaced by its matching interface"
        )

        @JvmStatic
        @JvmOverloads
        fun buildUnusedLocalVariableRule() = Rule(
            UnusedLocalVariableRule::class.java,
            "Unused local variable: `{0}`"
        )

        @JvmStatic
        @JvmOverloads
        fun buildUnusedPrivateFieldRule() = Rule(
            UnusedPrivateFieldRule::class.java,
            "Unused private field: `{0}`"
        )

        @JvmStatic
        @JvmOverloads
        fun buildUnusedPrivateMethodRule() = Rule(
            UnusedPrivateMethodRule::class.java,
            "Unused private method: `{0}`"
        )

        @JvmStatic
        @JvmOverloads
        fun buildClassNamingConventionsRule() = Rule(
            ClassNamingConventionsRule::class.java,
            "Class name should follow UpperCamelCase convention, but `{1}` found instead"
        )

        @JvmStatic
        @JvmOverloads
        fun buildMethodNamingConventionsRule() = Rule(
            MethodNamingConventionsRule::class.java,
            "Method name should follow lowerCamelCase convention, but `{1}` found instead"
        )

        @JvmStatic
        @JvmOverloads
        fun buildEmptyControlStatementRule() = Rule(
            EmptyControlStatementRule::class.java
        )



    }
}
