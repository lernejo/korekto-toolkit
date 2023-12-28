package com.github.lernejo.korekto.toolkit

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.github.lernejo.korekto.toolkit.i18n.I18nTemplateResolver
import com.github.lernejo.korekto.toolkit.thirdparty.git.ExerciseCloner
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitNature
import com.github.lernejo.korekto.toolkit.thirdparty.github.GitHubContext
import com.github.lernejo.korekto.toolkit.thirdparty.github.GitHubNature
import org.eclipse.jgit.api.ResetCommand
import org.kohsuke.github.GHIssue
import org.kohsuke.github.GHIssueState
import org.slf4j.LoggerFactory
import java.io.File
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*

val objectMapper: ObjectMapper = ObjectMapper()
    .findAndRegisterModules()
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

class CloneStep(private val forcePull: Boolean, private val branch: String? = null, private val localRepo: Path? = null) : GradingStep<GradingContext> {
    override fun run(context: GradingContext) {
        if(localRepo != null) {
            context.exercise = Exercise(extractSlug(localRepo), localRepo)
        } else {
            val exercise =
                ExerciseCloner(context.configuration.workspace).gitClone(context.configuration.repoUrl, forcePull)
            context.exercise = exercise

            branch?.let { branchName ->
                exercise
                    .lookupNature(GitNature::class.java)
                    .get()
                    .inContextK {
                        it.git.reset().setMode(ResetCommand.ResetType.HARD).call()
                        it.checkout(branchName)
                    }
            }
        }
    }

    private fun extractSlug(path: Path) = path.toString().split(File.separator).takeLast(2).joinToString("/")

    override fun close(context: GradingContext) {
        context.exercise?.close()
    }
}

data class Payload(val action: String, val details: GradeDetails)

class StoreResultsLocally : GradingStep<GradingContext> {
    override fun run(context: GradingContext) {
        val content = objectMapper.writeValueAsString(Payload("grading", context.gradeDetails))
        val outputFilePath = context.configuration.workspace.resolve(context.exercise?.name + ".json")
        if (!Files.exists(outputFilePath.parent)) {
            Files.createDirectories(outputFilePath.parent)
        }

        outputFilePath.toFile().writeText(content)
    }
}

class SendStep : GradingStep<GradingContext> {
    private val logger = LoggerFactory.getLogger(SendStep::class.java)

    override fun run(context: GradingContext) {
        val content = objectMapper.writeValueAsString(Payload("grading", context.gradeDetails))
        val client = HttpClient.newHttpClient()
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(context.configuration.callbackUrl))
            .POST(HttpRequest.BodyPublishers.ofString(content, StandardCharsets.UTF_8))

        if (context.configuration.callbackPassword != null) {
            val encodedPassword =
                Base64.getEncoder()
                    .encodeToString(context.configuration.callbackPassword.toByteArray(StandardCharsets.UTF_8))
            requestBuilder.header("Authorization", encodedPassword)
        }
        requestBuilder.header("Content-Type", "application/json")
        requestBuilder.header("X-GitHub-Event", "korekto")
        requestBuilder.header("X-GitHub-Delivery", UUID.randomUUID().toString())

        val request = requestBuilder.build()

        var error: Throwable? = null
        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val statusCode = response.statusCode()
            if (statusCode < 200 || statusCode > 299) {
                val body = response.body().trim { it <= ' ' }
                val bodyMessage = if (body.isNotEmpty()) "\nBody:\n$body" else " (empty body)"
                error =
                    java.lang.RuntimeException("Failed to send grade to callback url ${context.configuration.callbackUrl}  / HTTP code: $statusCode$bodyMessage")
            } else {
                logger.info("Successfully sent results, HTTP code: $statusCode")
            }
        } catch (e: ConnectException) {
            error =
                java.lang.RuntimeException("Failed to send grade to callback url ${context.configuration.callbackUrl}  / Message: ${e.message}")
        } catch (e: Exception) {
            error = java.lang.RuntimeException(
                "Failed to send grade to callback url ${context.configuration.callbackUrl}  / Message: ${e.message}",
                e
            )
        }
        if (error != null) {
            logger.info("Payload:\n$content")
            throw error
        }
    }
}

class UpsertGitHubGradingIssues(
    private val locale: Locale,
    private val deadline: (GradingContext) -> Instant?,
    private val dryRun: Boolean = false
) : GradingStep<GradingContext> {

    private val logger = LoggerFactory.getLogger(UpsertGitHubGradingIssues::class.java)

    override fun run(context: GradingContext) {
        val gitHubNature = context.exercise?.lookupNature(
            GitHubNature::class.java
        )
        if (gitHubNature?.isPresent == true && context.gradeDetails.parts.isNotEmpty()) {
            gitHubNature.get().withContext<Void?> { ghContext: GitHubContext ->
                val issues: List<GHIssue> = if (dryRun) listOf() else ghContext.repository.getIssues(GHIssueState.ALL)
                upsertDailyIssue(context, issues, ghContext)
                insertGGIssue(context, issues, ghContext)
                null
            }
        }
    }

    private fun insertGGIssue(context: GradingContext, issues: List<GHIssue>, ghContext: GitHubContext) {
        val titlePrefix = "[Korekto][GG]"
        val grade = context.gradeDetails.grade()
        val maxGrade = context.gradeDetails.maxGrade()
        val templateContext = mapOf(
            "grade" to grade,
            "maxGrade" to maxGrade,
        )
        if (grade == maxGrade) {
            val existingIssue = issues.stream()
                .filter { i: GHIssue -> i.title.startsWith(titlePrefix) }
                .findFirst()

            if (existingIssue.isEmpty) {
                val title =
                    titlePrefix + " " + I18nTemplateResolver().process("gg-issue/title", templateContext, locale).trim()
                val body = I18nTemplateResolver().process("gg-issue/body.md", templateContext, locale).trim()
                if (dryRun) {
                    logger.info("Should have created\n\t$title\n\n\t$body")
                } else {
                    val ghIssue = ghContext.repository.createIssue(title).body(body).create()
                    logger.info("Opened GG issue: " + ghIssue.htmlUrl)
                }
            }
        }
    }

    private fun upsertDailyIssue(
        context: GradingContext,
        issues: List<GHIssue>,
        ghContext: GitHubContext
    ) {
        val dailyTitlePrefix = "[Korekto][Daily]"
        val grade = context.gradeDetails.grade()
        val maxGrade = context.gradeDetails.maxGrade()
        val templateContext = mapOf(
            "grade" to grade,
            "maxGrade" to maxGrade,
            "gradeParts" to context.gradeDetails.parts,
            "deadline" to deadline(context),
            "now" to Instant.now()
        )
        val title =
            dailyTitlePrefix + " " + I18nTemplateResolver().process("live-issue/title", templateContext, locale).trim()
        val body = I18nTemplateResolver().process("live-issue/body.md", templateContext, locale).trim()
        val existingDailyIssue = issues.stream()
            .filter { i: GHIssue -> i.title.startsWith(dailyTitlePrefix) }
            .findFirst()
        val ghIssue: GHIssue?
        if (existingDailyIssue.isEmpty) {
            ghIssue = if (dryRun) {
                logger.info("Should have created\n\t$title\n\n\t$body")
                null
            } else {
                ghContext.repository.createIssue(title).body(body).create()
            }
        } else {
            if (existingDailyIssue.get().state == GHIssueState.CLOSED && grade != maxGrade) {
                ghIssue = ghContext.repository.createIssue(title).body(body).create()
            } else {
                ghIssue = existingDailyIssue.get()
                if (ghIssue.state != GHIssueState.CLOSED) {
                    if (!dryRun) {
                        if (ghIssue.body != body) {
                            ghIssue.body = body
                        }
                        if (ghIssue.title != title) {
                            ghIssue.title = title
                        }
                    }
                }
            }
            if (dryRun) {
                logger.info("Should have updated\n\t$title\n\n\t$body")
            }
        }
        if (grade == maxGrade && GHIssueState.OPEN == ghIssue?.state) {
            ghIssue.close()
        } else {
            if (!dryRun) {
                logger.info("Upserted Live issue: " + ghIssue!!.htmlUrl)
            }
        }
    }
}
