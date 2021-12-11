package com.github.lernejo.korekto.toolkit

import com.github.lernejo.korekto.toolkit.launcher.GradingJobLauncher
import com.github.lernejo.korekto.toolkit.misc.AsciiHistogram
import com.github.lernejo.korekto.toolkit.misc.HumanReadableDuration.toString
import com.github.lernejo.korekto.toolkit.misc.Maths
import com.github.lernejo.korekto.toolkit.misc.OS
import com.github.lernejo.korekto.toolkit.misc.Processes.launch
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.*
import java.util.stream.Collectors
import kotlin.math.max

class GradingJob(
    private val steps: List<NamedStep<GradingContext>> = emptyList(),
    private val onErrorListeners: List<OnErrorListener> = emptyList()
) {
    private val logger = LoggerFactory.getLogger(GradingJob::class.java)

    private fun insertPreStep(name: String, step: GradingStep<in GradingContext>) =
        GradingJob(listOf(NamedStep(name, step)).plus(steps), onErrorListeners)

    fun addStep(name: String, step: GradingStep<in GradingContext>) = GradingJob(steps.plus(NamedStep(name, step)), onErrorListeners)

    @JvmOverloads
    fun addCloneStep(forcePull: Boolean = true) = addStep("cloning", CloneStep(forcePull))

    @JvmOverloads
    fun addUpsertGitHubIssuesStep(locale: Locale, deadline: (GradingContext) -> Instant?, dryRun: Boolean = false) =
        addStep("upsert GH issues", UpsertGitHubGradingIssues(locale, deadline, dryRun))

    fun addSendStep() = addStep("sending results", SendStep())
    fun addStoreResultsLocallyStep() = addStep("writing results", StoreResultsLocally())

    fun addErrorListener(errorListener: OnErrorListener) =
        GradingJob(steps, onErrorListeners.plus(errorListener))

    @JvmOverloads
    fun runBatch(
        userSlugs: List<String>,
        repoUrlBuilder: (String) -> String,
        workspace: Path = Paths.get("target/repositories"),
        resetWorkspace: Boolean = false,
        contextSupplier: (GradingConfiguration) -> GradingContext
    ) {
        val total = userSlugs.size
        var failure = 0
        val start = System.currentTimeMillis()
        val gradesBySlug = mutableMapOf<String, Double>()

        if (resetWorkspace) {
            OS.CURRENT_OS?.deleteDirectoryCommand(workspace)?.let { launch(it, null) }
        }

        val failedSlugs = mutableListOf<String>()
        for (userSlug in userSlugs) {
            val gradingConfiguration = GradingConfiguration(
                repoUrlBuilder(userSlug),
                "",
                "",
                workspace
            )
            val enhancedJob: GradingJob =
                insertPreStep("add slug") { context -> context.data["slug"] = userSlug }
                    .addStep("record grade") { context -> gradesBySlug[userSlug] = context.gradeDetails.grade() }
                    .addErrorListener { _, _, _ -> gradesBySlug[userSlug] = 0.0 }

            val exitCode: Int = enhancedJob.run(gradingConfiguration, contextSupplier)

            if (exitCode != 0) {
                failedSlugs.add(userSlug)
                failure++
            }
        }
        val grades: List<Double> = gradesBySlug.values.toList()

        logger.info("All done in " + toString(System.currentTimeMillis() - start))
        logger.info("Success: " + (total - failure) + " / " + total)
        logger.info("Slugs without repository:\n\t" + failedSlugs.stream().collect(Collectors.joining("\n\t")))
        logger.info("Grades (total=" + grades.size + ") min=" + grades.minOrNull() + " max=" + grades.maxOrNull() + " avg=" + grades.average())
        logger.info("\n\n${AsciiHistogram().asciiHistogram(grades)}")

        logger.info(
            "\n\n\n" + gradesBySlug.entries.stream().map { e: Map.Entry<String, Double?> ->
                e.key.padEnd(40, ' ') + e.value.toString().replace('.', ',')
            }.collect(Collectors.joining("\n"))
        )
    }

    @JvmOverloads
    fun run(
        configuration: GradingConfiguration = GradingConfiguration(),
        contextSupplier: (GradingConfiguration) -> GradingContext = { GradingContext(it) }
    ): Int {
        val start = System.currentTimeMillis()
        val context = contextSupplier(configuration)
        val deque: Deque<(GradingContext) -> Unit> = LinkedList()
        var exitCode = 0
        for (namedStep in steps) {
            logger.debug("Start ${namedStep.name}...")
            val stepStart = System.currentTimeMillis()
            try {
                namedStep.action.run(context)
                deque.addFirst { namedStep.action.close(it) }
            } catch (e: Exception) {
                onErrorListeners.forEach { it.onError(e, configuration, context) }
                when (e) {
                    is WarningException -> if (e.cause == null) {
                        logger.warn(e.message)
                    } else {
                        logger.warn(e.message, e)
                    }
                    else -> logger.error(e.message, e)
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

fun interface GradingStep<T: GradingContext> {
    fun run(context: T)

    @JvmDefault
    fun close(context: T) {
    }
}

interface Grader<T: GradingContext> : GradingStep<T>, Closeable {

    fun slugToRepoUrl(slug: String): String

    fun needsWorkspaceReset() = false

    fun deadline(context: GradingContext): Instant? = null

    fun gradingContext(configuration: GradingConfiguration): T = GradingContext(configuration) as T

    override fun close() {}

    companion object {

        init {
            val properties = Properties()
            properties.load(GradingJobLauncher::class.java.classLoader.getResourceAsStream("project.properties"))
            println("Using KTK " + properties["project.version"])
        }

        fun load(): Grader<GradingContext>? {
            val serviceIterator = ServiceLoader.load(Grader::class.java).iterator()
            return if (serviceIterator.hasNext()) serviceIterator.next() as Grader<GradingContext>? else null
        }
    }
}

fun interface OnErrorListener {
    fun onError(ex: Exception, configuration: GradingConfiguration, context: GradingContext)
}

class WarningException(message: String, cause: java.lang.Exception) : RuntimeException(message, cause)

class GradingConfiguration(
    val repoUrl: String = System.getenv("REPO_URL"),
    val callbackUrl: String = System.getenv("CALLBACK_URL"),
    val callbackPassword: String? = System.getenv("CALLBACK_PASSWORD"),
    val workspace: Path = Paths.get("target/repositories")
)

open class GradingContext(val configuration: GradingConfiguration) : Closeable {
    var exercise: Exercise? = null
    val gradeDetails = GradeDetails()
    val data = mutableMapOf<String, Any>()

    override fun close() {
    }
}

data class GradeDetails(val parts: MutableList<GradePart> = ArrayList()) {
    fun grade() = max(Maths.round(parts.map { p -> p.grade }.sum(), 2), 0.0)
    fun maxGrade() = Maths.round(parts.map { p -> p.maxGrade ?: 0.0 }.sum(), 2)
}

data class GradePart(val id: String, val grade: Double, val maxGrade: Double?, val comments: List<String>)

data class NamedStep<T: GradingContext>(val name: String, val action: GradingStep<T>)
