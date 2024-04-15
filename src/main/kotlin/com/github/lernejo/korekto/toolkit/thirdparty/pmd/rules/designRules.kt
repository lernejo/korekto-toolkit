package com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules

import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration
import net.sourceforge.pmd.lang.java.ast.ASTTypeDeclaration
import net.sourceforge.pmd.lang.java.ast.JavaNode
import net.sourceforge.pmd.lang.java.ast.internal.PrettyPrintingUtil
import net.sourceforge.pmd.lang.java.metrics.JavaMetrics
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRulechainRule
import net.sourceforge.pmd.lang.metrics.Metric
import net.sourceforge.pmd.lang.metrics.MetricsUtil
import net.sourceforge.pmd.lang.rule.internal.CommonPropertyDescriptors
import net.sourceforge.pmd.properties.NumericConstraints
import net.sourceforge.pmd.properties.PropertyDescriptor

class ExcessiveClassLengthRule :
    AbstractJavaCounterCheckRule<ASTTypeDeclaration>(ASTTypeDeclaration::class.java, JavaMetrics.LINES_OF_CODE)

class ExcessiveMethodLengthRule :
    AbstractJavaCounterCheckRule<ASTMethodDeclaration>(ASTMethodDeclaration::class.java, JavaMetrics.NCSS)

abstract class AbstractJavaCounterCheckRule<T : JavaNode?>(
    nodeType: Class<T>,
    private val metric: Metric<JavaNode, Int>
) :
    AbstractJavaRulechainRule(nodeType) {
    private val reportLevel: PropertyDescriptor<Int> = CommonPropertyDescriptors.reportLevelProperty()
        .desc("Threshold above which a node is reported")
        .require(NumericConstraints.positive())
        .defaultValue(100_000)
        .build()

    init {
        definePropertyDescriptor(reportLevel)
    }

    override fun visitJavaNode(node: JavaNode, data: Any): Any {
        val threshold = getProperty(reportLevel)
        val count = MetricsUtil.computeMetric(metric, node)
        if (count > threshold) {
            asCtx(data).addViolation(
                node,
                PrettyPrintingUtil.getPrintableNodeKind(node),
                if (node is ASTMethodDeclaration) {
                    PrettyPrintingUtil.displaySignature(node)
                } else {
                    PrettyPrintingUtil.getNodeName(node)
                },
                threshold.toString(),
                count.toString()
            )
        }
        return data
    }
}
