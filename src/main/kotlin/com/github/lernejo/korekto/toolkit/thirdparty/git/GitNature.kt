package com.github.lernejo.korekto.toolkit.thirdparty.git

import com.github.lernejo.korekto.toolkit.*
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.errors.RefAlreadyExistsException
import org.eclipse.jgit.revwalk.RevCommit
import java.io.IOException
import java.util.*

class GitNature(val context: GitContext) : Nature<GitContext> {
    override fun <RESULT> withContext(action: (GitContext) -> RESULT): RESULT = action.invoke(context)

    override fun finalize() {
        context.git.repository.close()
        super.finalize()
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
