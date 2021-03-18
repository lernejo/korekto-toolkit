package com.github.lernejo.korekto.toolkit

import com.github.lernejo.korekto.toolkit.misc.AsciiHistogram
import com.github.lernejo.korekto.toolkit.misc.HumanReadableDuration.toString
import com.github.lernejo.korekto.toolkit.misc.OS
import com.github.lernejo.korekto.toolkit.misc.Processes.launch
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList

class GradingJob(
    private val steps: List<NamedStep> = emptyList(),
    private val onErrorListeners: List<OnErrorListener> = emptyList()
) {
    private val logger = LoggerFactory.getLogger(GradingJob::class.java)

    private fun insertPreStep(name: String, step: GradingStep) =
        GradingJob(listOf(NamedStep(name, step)).plus(steps), onErrorListeners)

    fun addStep(name: String, step: GradingStep) = GradingJob(steps.plus(NamedStep(name, step)), onErrorListeners)

    fun addCloneStep() = addStep("cloning", CloneStep())
    fun addUpsertGitHubIssuesStep(locale: Locale, deadline: (GradingContext) -> Instant) =
        addStep("upsert GH issues", UpsertGitHubGradingIssues(locale, deadline))

    fun addSendStep() = addStep("sending results", SendStep())
    fun addStoreResultsLocallyStep() = addStep("writing results", StoreResultsLocally())

    fun addErrorListener(errorListener: OnErrorListener) =
        GradingJob(steps, onErrorListeners.plus(errorListener))

    @JvmOverloads
    fun runBatch(
        userSlugs: List<String>,
        repoUrlBuilder: (String) -> String,
        workspace: Path = Paths.get("target/repositories")
    ) {
        val total = userSlugs.size
        var failure = 0
        val start = System.currentTimeMillis()
        val gradesBySlug = mutableMapOf<String, Double>()

        OS.CURRENT_OS?.deleteDirectoryCommand(workspace)?.let { launch(it, null) }

        for (userSlug in userSlugs) {
            val gradingConfiguration = GradingConfiguration(
                repoUrlBuilder(userSlug),
                "",
                "",
                workspace
            )
            val enhancedJob: GradingJob =
                insertPreStep("add slug") { _, context -> context.data["slug"] = userSlug }
                    .addStep("record grade") { _, context -> gradesBySlug[userSlug] = context.gradeDetails.grade() }
                    .addErrorListener { _, _, _ -> gradesBySlug[userSlug] = 0.0 }

            val exitCode: Int = enhancedJob.run(gradingConfiguration)

            if (exitCode != 0) {
                failure++
            }
        }
        val grades: List<Double> = gradesBySlug.values.toList()

        logger.info("All done in " + toString(System.currentTimeMillis() - start))
        logger.info("Success: " + (total - failure) + " / " + total)
        logger.info("Grades (total=" + grades.size + ") min=" + grades.minOrNull() + " max=" + grades.maxOrNull() + " avg=" + grades.average())
        logger.info("\n\n${AsciiHistogram().asciiHistogram(grades)}")

        logger.info(
            "\n\n\n" + gradesBySlug.entries.stream().map { e: Map.Entry<String, Double?> ->
                e.key.padEnd(40, ' ') + e.value.toString().replace('.', ',')
            }.collect(Collectors.joining("\n"))
        )
    }

    @JvmOverloads
    fun run(configuration: GradingConfiguration = GradingConfiguration()): Int {
        val start = System.currentTimeMillis()
        val context = GradingContext()
        val deque: Deque<(GradingContext) -> Unit> = LinkedList()
        var exitCode = 0
        for (namedStep in steps) {
            logger.debug("Start ${namedStep.name}...")
            val stepStart = System.currentTimeMillis()
            try {
                namedStep.action.run(configuration, context)
                deque.addFirst { namedStep.action.close(it) }
            } catch (e: RuntimeException) {
                onErrorListeners.forEach { it.onError(e, configuration, context) }
                when (e) {
                    is WarningException -> if (e.cause == null) {
                        logger.warn(e.message)
                    } else {
                        logger.warn(e.message, e)
                    }
                    else -> if (e.cause == null) {
                        logger.error(e.message)
                    } else {
                        logger.error(e.message, e)
                    }
                }
                exitCode = 1
                break
            } finally {
                logger.debug("Done ${namedStep.name} in " + toString(System.currentTimeMillis() - stepStart))
            }
        }
        while (deque.peekFirst() != null) {
            deque.pollFirst().invoke(context)
        }

        logger.info("Total in " + toString(System.currentTimeMillis() - start))

        return exitCode
    }
}

fun interface GradingStep {
    fun run(configuration: GradingConfiguration, context: GradingContext)

    @JvmDefault
    fun close(context: GradingContext) {
    }
}

fun interface OnErrorListener {
    fun onError(ex: RuntimeException, configuration: GradingConfiguration, context: GradingContext)
}

class WarningException(message: String, cause: java.lang.Exception) : RuntimeException(message, cause)

class GradingConfiguration(
    val repoUrl: String = System.getenv("REPO_URL"),
    val callbackUrl: String = System.getenv("CALLBACK_URL"),
    val callbackPassword: String? = System.getenv("CALLBACK_PASSWORD"),
    val workspace: Path = Paths.get("target/repositories")
)

class GradingContext {
    var exercise: Exercise? = null
    val gradeDetails = GradeDetails()
    val data = mutableMapOf<String, Any>()
}

data class GradeDetails(val parts: MutableList<GradePart> = ArrayList()) {
    fun grade() = parts.map { p -> p.grade }.sum()
    fun maxGrade() = parts.map { p -> p.maxGrade }.sum()
}

data class GradePart(val id: String, val grade: Double, val maxGrade: Double, val comments: List<String>)

data class NamedStep(val name: String, val action: GradingStep)
