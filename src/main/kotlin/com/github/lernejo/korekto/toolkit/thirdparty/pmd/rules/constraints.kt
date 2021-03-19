package com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules

import net.sourceforge.pmd.properties.constraints.PropertyConstraint
import org.apache.commons.lang3.StringUtils
import java.util.function.Predicate

fun <U> fromPredicate(pred: Predicate<in U>, constraintDescription: String): PropertyConstraint<U> {
    return object : PropertyConstraint<U> {
        override fun test(value: U): Boolean {
            return pred.test(value)
        }

        override fun validate(value: U): String? {
            return if (pred.test(value)) null else "Constraint violated on property value '$value' ($constraintDescription)"
        }

        override fun getConstraintDescription(): String {
            return StringUtils.capitalize(constraintDescription)
        }

        override fun toCollectionConstraint(): PropertyConstraint<Iterable<U>> {
            val thisValidator: PropertyConstraint<U> = this
            return fromPredicate(
                { us: Iterable<U> -> us.all { thisValidator.test(it) } },
                "Components " + StringUtils.uncapitalize(thisValidator.constraintDescription)
            )
        }
    }
}
