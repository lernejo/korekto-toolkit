package com.github.lernejo.korekto.toolkit.thirdparty.pmd

class PmdReportTranslator: (List<PmdReport>, Array<out Rule>) -> PmdResult {
    override fun invoke(pmdReports: List<PmdReport>, rules: Array<out Rule>): PmdResult {
        val ruleTolerances = HashMap<String, Int>()
        rules.filter { it.exceptions > 0 }.forEach { ruleTolerances[it.name!!] = it.exceptions }

        if (pmdReports.isEmpty()) {
            return PmdResult(0,  listOf("No analysis can be performed"))
        }

        val sortedReports = pmdReports.sortedBy { r -> r.fileReports.size }

        var violations = 0
        val messages: MutableList<String> = mutableListOf()

        for (pmdReport in sortedReports) {
            val fileReports = pmdReport.fileReports.sortedBy { fr -> fr.name }

            fileReports
                .map { fr: FileReport ->
                    buildViolationsBlock(
                        ruleTolerances,
                        fr
                    )
                }
                .filter { it.violations > 0 }
                .map {
                    violations += it.violations
                    it.fileReportName + it.report
                }
                .forEach { e -> messages.add(e) }
        }

        if (messages.isEmpty()) {
            messages.add("OK")
        }

        return PmdResult(violations, messages.toList())
    }
}

data class PmdResult(val violationCount: Int, val messages: List<String>)

private data class ViolationBlock(val fileReportName: String, val report: String, val violations: Int)

private fun buildViolationsBlock(ruleTolerances: HashMap<String, Int>, fileReport: FileReport): ViolationBlock {
    val violations = fileReport
        .violations
        .filter {
            if (ruleTolerances.containsKey(it.rule)) {
                ruleTolerances.compute(it.rule) { _: String, v: Int? -> v!! - 1 }
            }
            !ruleTolerances.containsKey(it.rule) || ruleTolerances[it.rule]!! < 0
        }
    val report = violations
        .sortedBy { it.beginLine * 10000 + it.beginColumn }.joinToString(
            "\n            * ",
            "\n            * ",
            ""
        ) { v -> "L." + v.beginLine + ": " + v.message.trim { it <= ' ' } }
    return ViolationBlock(fileReport.name, report, violations.size)
}
