package com.github.lernejo.korekto.toolkit

import com.github.lernejo.korekto.toolkit.misc.HumanReadableDuration.toString
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.collections.ArrayList

class GradingJob(
    private val steps: List<NamedStep> = emptyList(),
    private val onErrorListeners: List<OnErrorListener> = emptyList()
) {
    private val logger = LoggerFactory.getLogger(GradingJob::class.java)

    fun addCloneStep() = addStep("cloning", CloneStep())
    fun addStep(name: String, step: GradingStep) = GradingJob(steps.plus(NamedStep(name, step)))

    fun addSendStep() = addStep("sending results", SendStep())
    fun addStoreResultsLocallyStep() = addStep("writing results", StoreResultsLocally())
    fun addErrorListener(errorListener: OnErrorListener) = GradingJob(steps, onErrorListeners.plus(errorListener))

    @JvmOverloads
    fun run(configuration: GradingConfiguration = GradingConfiguration()): Int {
        val start = System.currentTimeMillis()
        val context = GradingContext()
        val deque: Deque<(GradingContext) -> Unit> = LinkedList()
        var exitCode = 0
        for (namedStep in steps) {
            logger.info("Start ${namedStep.name}...")
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
                logger.info("Done ${namedStep.name} in " + toString(System.currentTimeMillis() - stepStart))
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
}

data class GradeDetails(val parts: MutableList<GradePart> = ArrayList())

data class GradePart(val id: String, val grade: Double, val comments: List<String>)

data class NamedStep(val name: String, val action: GradingStep)
