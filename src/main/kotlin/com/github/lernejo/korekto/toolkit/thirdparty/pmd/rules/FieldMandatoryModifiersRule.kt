package com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules

import net.sourceforge.pmd.lang.ast.Node
import net.sourceforge.pmd.lang.java.ast.*
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule
import net.sourceforge.pmd.properties.PropertyDescriptor
import net.sourceforge.pmd.properties.PropertyFactory
import org.apache.commons.lang3.StringUtils
import java.util.*

class FieldMandatoryModifiersRule : AbstractJavaRule() {
    private val modifiersToComply: Set<ModifierMeta> by lazy {
        getProperty(MODIFIERS_PROPERTY).map { descriptor: String -> ModifierMeta(descriptor) }.toSet()
    }

    override fun visit(node: ASTFieldDeclaration, data: Any): Any {
        val modifiers: MutableSet<Modifier> = EnumSet.noneOf(
            Modifier::class.java
        )
        if (node.isSyntacticallyPublic) {
            modifiers.add(Modifier.PUBLIC)
        }
        if (node.isSyntacticallyStatic) {
            modifiers.add(Modifier.STATIC)
        }
        if (node.isSyntacticallyFinal) {
            modifiers.add(Modifier.FINAL)
        }
        if (node.isPrivate) {
            modifiers.add(Modifier.PRIVATE)
        }
        if (node.isAbstract) {
            modifiers.add(Modifier.ABSTRACT)
        }
        if (node.isProtected) {
            modifiers.add(Modifier.PROTECTED)
        }
        val missings: MutableSet<Modifier> = EnumSet.noneOf(
            Modifier::class.java
        )
        val illegals: MutableSet<Modifier> = EnumSet.noneOf(
            Modifier::class.java
        )
        modifiersToComply.forEach { m ->
            if ( m.forbidden && modifiers.contains(m.modifier)) {
                illegals.add(m.modifier!!)
            } else if (!m.forbidden && !modifiers.contains(m.modifier)) {
                missings.add(m.modifier!!)
            }
        }
        if (missings.isNotEmpty() || illegals.isNotEmpty()) {
            addViolationWithMessage(data, node, formatExplanation(node, missings, illegals))
        }
        return data
    }

    private fun formatExplanation(
        node: ASTFieldDeclaration,
        missings: Set<Modifier?>,
        illegals: Set<Modifier?>
    ): String {
        val sb = StringBuilder("The ").append(getPrintableNodeKind(node))
        if (missings.isNotEmpty()) {
            sb.append(" must have modifier" + formatModifiers(missings))
        }
        if (illegals.isNotEmpty()) {
            if (missings.isNotEmpty()) {
                sb.append(" and")
            }
            sb.append(" must not have modifier" + formatModifiers(illegals))
        }
        return sb.toString()
    }

    private fun formatModifiers(set: Set<Modifier?>): String {
        // prints in the standard modifier order (sorted by enum constant ordinal),
        // regardless of the actual order in which we checked
        return (if (set.size > 1) "s" else "") + " `" + StringUtils.join(set, "`, `") + "`"
    }

    private fun getPrintableNodeKind(node: Node): String {
        return when (node) {
            is ASTAnyTypeDeclaration -> {
                node.typeKind.printableName
            }
            is MethodLikeNode -> {
                node.kind.printableName
            }
            is ASTFieldDeclaration -> {
                "field `" + node.getFirstChildOfType(ASTVariableDeclarator::class.java).name + '`'
            }
            is ASTResource -> {
                "resource specification"
            }
            else -> throw UnsupportedOperationException("Node $node is unaccounted for")
        }
    }

    internal enum class Modifier {
        PUBLIC, PRIVATE, PROTECTED, STATIC, FINAL, ABSTRACT;

        override fun toString(): String {
            return name.lowercase(Locale.ROOT)
        }
    }

    internal class ModifierMeta constructor(descriptor: String) {
        internal val forbidden: Boolean = startsWithExclamation(descriptor)
        internal var modifier: Modifier? = null

        companion object {
            private val NAMES = Modifier.values().map { obj: Modifier -> obj.name }
                .map { obj: String -> obj.lowercase(Locale.getDefault()) }.toSet()

            private fun startsWithExclamation(descriptor: String): Boolean {
                return descriptor.isNotEmpty() && descriptor[0] == '!'
            }

            internal fun isValid(descriptor: String): Boolean {
                return NAMES.contains(descriptor) || descriptor.startsWith("!") && NAMES.contains(descriptor.substring(1))
            }
        }

        init {
            modifier = if (forbidden) {
                Modifier.valueOf(descriptor.substring(1).uppercase(Locale.getDefault()))
            } else {
                Modifier.valueOf(descriptor.uppercase(Locale.getDefault()))
            }
        }
    }

    companion object {
        private val MODIFIERS_PROPERTY: PropertyDescriptor<List<String>> =
            PropertyFactory.stringListProperty("modifiers")
                .desc("List of field mandatory modifiers, ! to forbid them")
                .defaultValue(listOf("final", "!static"))
                .requireEach(
                    fromPredicate(
                        { descriptor: String -> ModifierMeta.isValid(descriptor) },
                        "Should be contained in " + listOf(*Modifier.values())
                    )
                )
                .build()
    }

    init {
        definePropertyDescriptor(MODIFIERS_PROPERTY)
        addRuleChainVisit(ASTFieldDeclaration::class.java)
    }
}
