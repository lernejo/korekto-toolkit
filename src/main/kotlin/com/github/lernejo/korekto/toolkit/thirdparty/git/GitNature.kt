package com.github.lernejo.korekto.toolkit.thirdparty.git

import com.github.lernejo.korekto.toolkit.Exercise
import com.github.lernejo.korekto.toolkit.Nature
import com.github.lernejo.korekto.toolkit.NatureContext
import com.github.lernejo.korekto.toolkit.NatureFactory
import com.github.lernejo.korekto.toolkit.misc.Distances
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.errors.RefAlreadyExistsException
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.IOException
import java.util.*

class GitNature(val context: GitContext) : Nature<GitContext> {
    override fun <RESULT> withContext(action: (GitContext) -> RESULT): RESULT = action.invoke(context)

    override fun close() {
        context.git.repository.close()
        super.close()
    }
}

class GitContext(val git: Git) : NatureContext {
    val branchNames: Set<String> by lazy {
        git.branchList()
            .setListMode(ListBranchCommand.ListMode.ALL)
            .call()
            .map { r ->
                r.name
                    .replace("refs/remotes/origin/", "")
                    .replace("refs/heads/", "")
            }
            .toSet()
    }

    fun currentBranchName() = git.repository.branch

    fun checkout(branchName: String) {
        try {
            git.checkout()
                .setCreateBranch(true)
                .setName(branchName)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                .setStartPoint("origin/$branchName")
                .call()
        } catch (e: RefAlreadyExistsException) {
            git.checkout()
                .setName(branchName)
                .call()
        }
    }

    fun listOrderedCommits(): List<RevCommit> = git.log().call().reversed().toList()

    @JvmOverloads
    fun meaninglessCommits(minWords: Int = 2, messageMinDistance: Int = 5): List<MeaninglessCommit> {
        val orderedCommits = listOrderedCommits()
        val meaninglessCommits: MutableList<MeaninglessCommit> = ArrayList<MeaninglessCommit>()
        var previousCommit: Commit? = null
        for (commit in orderedCommits) {
            val message = commit.shortMessage
            val wordCount: Int = Distances.countWords(message)
            if (wordCount < minWords) {
                meaninglessCommits.add(
                    MeaninglessCommit(
                        commit,
                        wordCount.toString() + " word" + (if (wordCount == 1) "" else "s") + " is too short"
                    )
                )
            } else if (message.length < 10) {
                meaninglessCommits.add(MeaninglessCommit(commit, message.length.toString() + " chars is too short"))
            } else if (previousCommit != null && (Distances.levenshteinDistance(
                    previousCommit.message,
                    message
                ) < messageMinDistance)
            ) {
                meaninglessCommits.add(MeaninglessCommit(commit, "Should be squashed on ${previousCommit.shortId}"))
            }
            previousCommit = Commit(commit)
        }
        return meaninglessCommits
    }
}

class GitNatureFactory : NatureFactory {
    override fun getNature(exercise: Exercise): Optional<Nature<*>> {
        return try {
            Optional.of(GitNature(GitContext(Git.open(exercise.root.toFile()))))
        } catch (e: IOException) {
            Optional.empty()
        }
    }
}

private data class Commit(val shortId: String, val message: String) {
    constructor(commit: RevCommit) : this(commit.id.abbreviate(7).name(), commit.shortMessage)
}

data class MeaninglessCommit(val shortId: String, val message: String, val reason: String) {
    constructor(commit: RevCommit, reason: String) : this(commit.id.abbreviate(7).name(), commit.shortMessage, reason)
}
