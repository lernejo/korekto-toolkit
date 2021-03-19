package com.github.lernejo.korekto.toolkit.i18n

import com.github.lernejo.korekto.toolkit.misc.Maths
import org.thymeleaf.context.IExpressionContext
import org.thymeleaf.standard.expression.AbstractStandardConversionService
import kotlin.math.round

class CustomStandardConversionService : AbstractStandardConversionService() {

    override fun convertToString(context: IExpressionContext?, value: Any?): String = super.convertToString(
        context, when (value) {
            is Double -> if (round(value) == value) value.toInt() else Maths.round(value, 2)
            else -> value
        }
    )
}
