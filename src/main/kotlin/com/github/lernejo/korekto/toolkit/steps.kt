package com.github.lernejo.korekto.toolkit

import com.github.lernejo.korekto.toolkit.thirdparty.git.ExerciseCloner
import com.google.gson.Gson
import org.slf4j.LoggerFactory
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class CloneStep : GradingStep {
    override fun run(configuration: GradingConfiguration, context: GradingContext) {
        context.exercise = ExerciseCloner(configuration.workspace).gitClone(configuration.repoUrl)
    }

    override fun close(context: GradingContext) {
        context.exercise?.close()
    }
}

data class Payload(val action: String, val details: GradeDetails)

class StoreResultsLocally : GradingStep {
    override fun run(configuration: GradingConfiguration, context: GradingContext) {
        val content = Gson().toJson(Payload("grading", context.gradeDetails))
        val outputFilePath = configuration.workspace.resolve(context.exercise?.name + ".json")
        if (!Files.exists(outputFilePath.parent)) {
            Files.createDirectories(outputFilePath.parent);
        }

        outputFilePath.toFile().writeText(content)
    }
}

class SendStep : GradingStep {
    private val logger = LoggerFactory.getLogger(SendStep::class.java)

    override fun run(configuration: GradingConfiguration, context: GradingContext) {
        val content = Gson().toJson(Payload("grading", context.gradeDetails))
        val client = HttpClient.newHttpClient()
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(configuration.callbackUrl))
            .POST(HttpRequest.BodyPublishers.ofString(content, StandardCharsets.UTF_8))

        if (configuration.callbackPassword != null) {
            val encodedPassword =
                Base64.getEncoder().encodeToString(configuration.callbackPassword.toByteArray(StandardCharsets.UTF_8))
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
                    java.lang.RuntimeException("Failed to send grade to callback url ${configuration.callbackUrl}  / HTTP code: $statusCode$bodyMessage")
            } else {
                logger.info("Successfully sent results, HTTP code: $statusCode")
            }
        } catch (e: ConnectException) {
            error =
                java.lang.RuntimeException("Failed to send grade to callback url ${configuration.callbackUrl}  / Message: ${e.message}")
        } catch (e: Exception) {
            error = java.lang.RuntimeException(
                "Failed to send grade to callback url ${configuration.callbackUrl}  / Message: ${e.message}",
                e
            )
        }
        if (error != null) {
            logger.info("Payload:\n$content")
            throw error
        }
    }
}
