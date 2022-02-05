package com.github.lernejo.korekto.toolkit.partgrader

import com.github.lernejo.korekto.toolkit.GradePart
import com.github.lernejo.korekto.toolkit.GradingContext
import com.github.lernejo.korekto.toolkit.PartGrader
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitNature
import kotlin.math.max

class GitHistoryPartGrader<T : GradingContext>(
    private val name: String,
    private val minGrade: Double
) : PartGrader<T> {
    override fun name() = name
    override fun minGrade() = minGrade

    override fun grade(context: T): GradePart {
        val gitContext = context.exercise?.lookupNature(GitNature::class.java)?.get()?.context!!
        val meaninglessCommits = gitContext.meaninglessCommits()
        val messages = meaninglessCommits
            .map { (shortId, message, reason) -> "`$shortId` $message --> $reason" }
            .toMutableList()
        if (messages.isEmpty()) {
            messages.add("OK")
        }
        return GradePart(name(), max(meaninglessCommits.size * minGrade() / 8, minGrade()), null, messages)
    }
}
