package com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules

import net.sourceforge.pmd.stat.DataPoint

class ExcessiveMethodLengthRule : net.sourceforge.pmd.lang.java.rule.design.ExcessiveMethodLengthRule() {
    override fun getViolationParameters(point: DataPoint) = arrayOf(point.score)
}

class ExcessiveClassLengthRule : net.sourceforge.pmd.lang.java.rule.design.ExcessiveClassLengthRule() {
    override fun getViolationParameters(point: DataPoint) = arrayOf(point.score)
}
