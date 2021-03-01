package com.github.lernejo.korekto.toolkit.thirdparty.github

import com.github.lernejo.korekto.toolkit.Exercise
import com.github.lernejo.korekto.toolkit.Nature
import com.github.lernejo.korekto.toolkit.NatureContext
import com.github.lernejo.korekto.toolkit.NatureFactory
import okhttp3.OkHttpClient
import org.eclipse.jgit.api.Git
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.kohsuke.github.extras.okhttp3.OkHttpConnector
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class GitHubNature(val context: GitHubContext) : Nature<GitHubContext> {
    override fun <RESULT> withContext(action: (GitHubContext) -> RESULT): RESULT = action.invoke(context)

    override fun finalize() {
        super.finalize()
    }
}

class GitHubContext(val gitHub: GitHub, val repository: GHRepository) : NatureContext

class GitHubNatureFactory : NatureFactory {
    override fun getNature(exercise: Exercise): Optional<Nature<*>> {
        return try {
            val git = Git.open(exercise.root.toFile())
            val gitHubRemote = git.remoteList().call()
                .flatMap { r -> r.urIs }
                .filter { uri -> uri.isRemote }
                .any { uri -> "github.com" == uri.host }
            git.close()
            val token = System.getProperty("github_token")
            if (gitHubRemote && token != null) {
                val gitHub = GitHubBuilder()
                    .withOAuthToken(token)
                    .withConnector(
                        OkHttpConnector(
                            OkHttpClient.Builder()
                                .readTimeout(2L, TimeUnit.SECONDS)
                                .build()
                        )
                    )
                    .build()
                Optional.of(GitHubNature(GitHubContext(gitHub, gitHub.getRepository(exercise.name))))
            } else {
                Optional.empty()
            }
        } catch (e: IOException) {
            Optional.empty()
        }
    }
}
