package com.github.lernejo.korekto.toolkit.partgrader

import com.github.lernejo.korekto.toolkit.GradePart
import com.github.lernejo.korekto.toolkit.GradingContext
import com.github.lernejo.korekto.toolkit.PartGrader
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitNature
import com.github.lernejo.korekto.toolkit.thirdparty.github.GitHubNature
import com.github.lernejo.korekto.toolkit.thirdparty.github.WorkflowRunConclusion
import com.github.lernejo.korekto.toolkit.thirdparty.github.WorkflowRunStatus
import java.io.IOException
import java.util.*

class GitHubActionsPartGrader<T : GradingContext>(
    private val name: String,
    private val maxGrade: Double
) : PartGrader<T> {
    override fun name() = name
    override fun maxGrade() = maxGrade

    override fun grade(context: T): GradePart {
        val gitNature = context.exercise?.lookupNature(GitNature::class.java)?.get()!!
        val branch = gitNature.context.currentBranchName()
        val gitHubNature: Optional<GitHubNature> = context.exercise?.lookupNature(GitHubNature::class.java)!!
        if (gitHubNature.isEmpty) {
            return result(listOf("Not a GitHub project"), 0.0)
        }
        try {
            val actionRuns = gitHubNature.get().listActionRuns()

            val currentBranchRuns = actionRuns
                .filter { ar -> ar.status === WorkflowRunStatus.completed }
                .filter { ar -> ar.head_branch == branch }

            return if (currentBranchRuns.isEmpty()) {
                result(
                    listOf("No CI runs for `$branch` branch, check https://github.com/${context.exercise?.name}/actions"),
                    0.0
                )
            } else {
                val conclusion = currentBranchRuns[0].conclusion
                if (conclusion !== WorkflowRunConclusion.success) {
                    result(
                        listOf("Latest CI run of branch `$branch` was expected to be in *success* state but found: $conclusion"),
                        maxGrade() / 2
                    )
                } else {
                    result(listOf(), maxGrade())
                }
            }
        } catch (e: IOException) {
            return result(
                listOf("Missing permission to access GitHub actions read-only API"),
                0.0
            )
        }
    }
}
