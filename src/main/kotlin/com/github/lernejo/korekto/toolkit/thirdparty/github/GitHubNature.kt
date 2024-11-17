package com.github.lernejo.korekto.toolkit.thirdparty.github

import com.github.lernejo.korekto.toolkit.*
import okhttp3.OkHttpClient
import org.eclipse.jgit.api.Git
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger(GitHubNature::class.java)

class GitHubNature(val context: GitHubContext) : Nature<GitHubContext> {
    override fun <RESULT> withContext(action: (GitHubContext) -> RESULT): RESULT = action.invoke(context)

    fun listActionRuns(): List<WorkflowRun> {
        val requestURL = "https://api.github.com/repos/${context.exerciseName}/actions/runs"
        val url = URI(requestURL).toURL()
        val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
        GitHubAuthenticationHolder.auth.configure(conn)

        return conn.inputStream.use { `is` ->
            Scanner(`is`, StandardCharsets.UTF_8).use { scanner ->
                val jsonString = scanner.useDelimiter("\\A").next()
                objectMapper.readValue(jsonString, ActionRunsResponse::class.java).workflow_runs
            }
        }
    }
}

@Suppress("PropertyName")
data class ActionRunsResponse(val workflow_runs: List<WorkflowRun>)

@Suppress("PropertyName")
data class WorkflowRun(
    val name: String,
    val head_branch: String,
    val status: WorkflowRunStatus,
    val conclusion: WorkflowRunConclusion?
)

@Suppress("EnumEntryName", "unused")
enum class WorkflowRunStatus {
    queued, in_progress, completed
}

@Suppress("EnumEntryName", "unused")
enum class WorkflowRunConclusion {
    action_required, cancelled, failure, neutral, success, skipped, stale, startup_failure, timed_out
}

@Suppress("MemberVisibilityCanBePrivate")
class GitHubContext(val gitHub: GitHub, val exerciseName: String) : NatureContext {
    val repository: GHRepository by lazy {
        logger.debug("[gh-client] Loading repository $exerciseName")
        gitHub.getRepository(exerciseName)
    }
}

class GitHubNatureFactory : NatureFactory {
    private fun createClient(): GitHub {
        val builder = GitHubBuilder()
            .withConnector(
                OkHttpGitHubConnector(
                    OkHttpClient.Builder()
                        .readTimeout(2L, TimeUnit.SECONDS)
                        .build()
                )
            )
        GitHubAuthenticationHolder.auth.configure(builder)
        logger.debug("[gh-client] Creating the GitHub client (type: " + GitHubAuthenticationHolder.auth.type + ")")
        return builder.build()
    }
    override fun getNature(exercise: Exercise): Optional<Nature<*>> {
        return try {
            val git = Git.open(exercise.root.toFile())
            val gitHubRemote = git.remoteList().call()
                .flatMap { r -> r.urIs }
                .filter { uri -> uri.isRemote }
                .any { uri -> "github.com" == uri.host }
            git.close()
            if (gitHubRemote) {
                Optional.of(GitHubNature(GitHubContext(createClient(), exercise.name)))
            } else {
                Optional.empty()
            }
        } catch (e: IOException) {
            Optional.empty()
        }
    }
}
