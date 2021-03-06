package com.github.lernejo.korekto.toolkit.thirdparty.github

import com.github.lernejo.korekto.toolkit.Exercise
import com.github.lernejo.korekto.toolkit.Nature
import com.github.lernejo.korekto.toolkit.NatureContext
import com.github.lernejo.korekto.toolkit.NatureFactory
import com.google.gson.Gson
import okhttp3.OkHttpClient
import org.eclipse.jgit.api.Git
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.kohsuke.github.extras.okhttp3.OkHttpConnector
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit

internal object GitHubClientHolder {
    val client: GitHub by lazy {
        val builder = GitHubBuilder()
            .withConnector(
                OkHttpConnector(
                    OkHttpClient.Builder()
                        .readTimeout(2L, TimeUnit.SECONDS)
                        .build()
                )
            )
        val token = System.getProperty("github_token")
        if (token != null) {
            builder.withOAuthToken(token)
        }
        builder.build()
    }
}

class GitHubNature(val context: GitHubContext) : Nature<GitHubContext> {
    override fun <RESULT> withContext(action: (GitHubContext) -> RESULT): RESULT = action.invoke(context)

    override fun finalize() {
        super.finalize()
    }

    fun listActionRuns(): List<WorkflowRun> {
        val requestURL = "https://api.github.com/repos/${context.exerciseName}/actions/runs"
        val url = URL(requestURL)
        val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
        val token = System.getProperty("github_token")
        if (token != null) {
            conn.setRequestProperty("Authorization", "token $token");
        }

        return conn.inputStream.use { `is` ->
            Scanner(`is`, StandardCharsets.UTF_8).use { scanner ->
                val jsonString = scanner.useDelimiter("\\A").next()
                Gson().fromJson(jsonString, ActionRunsResponse::class.java).workflow_runs
            }
        }
    }
}

data class ActionRunsResponse(val workflow_runs: List<WorkflowRun>)
data class WorkflowRun(
    val name: String,
    val head_branch: String,
    val status: WorkflowRunStatus,
    val conclusion: WorkflowRunConclusion?
)

enum class WorkflowRunStatus {
    queued, in_progress, completed
}

enum class WorkflowRunConclusion {
    action_required, cancelled, failure, neutral, success, skipped, stale, timed_out
}

class GitHubContext(val gitHub: GitHub, val exerciseName: String) : NatureContext {
    val repository: GHRepository by lazy {
        gitHub.getRepository(exerciseName)
    }
}

class GitHubNatureFactory : NatureFactory {
    override fun getNature(exercise: Exercise): Optional<Nature<*>> {
        return try {
            val git = Git.open(exercise.root.toFile())
            val gitHubRemote = git.remoteList().call()
                .flatMap { r -> r.urIs }
                .filter { uri -> uri.isRemote }
                .any { uri -> "github.com" == uri.host }
            git.close()
            if (gitHubRemote) {
                Optional.of(GitHubNature(GitHubContext(GitHubClientHolder.client, exercise.name)))
            } else {
                Optional.empty()
            }
        } catch (e: IOException) {
            Optional.empty()
        }
    }
}
