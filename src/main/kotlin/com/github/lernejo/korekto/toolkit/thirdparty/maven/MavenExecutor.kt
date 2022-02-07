package com.github.lernejo.korekto.toolkit.thirdparty.maven

import com.github.lernejo.korekto.toolkit.Exercise
import com.github.lernejo.korekto.toolkit.thirdparty.maven.invoker.CustomerInvoker
import org.apache.maven.shared.invoker.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask


class MavenExecutionHandle(private val futureTask: FutureTask<MavenInvocationResult>, private val thread: Thread) :
    AutoCloseable {

    fun closeAndReturn(): MavenInvocationResult {
        if (!thread.isInterrupted) {
            thread.interrupt()
            while (!futureTask.isDone) {
                Thread.sleep(100L)
            }
        }
        return futureTask.get()
    }

    override fun close() {
        closeAndReturn()
    }
}

object MavenExecutor {

    @JvmStatic
    fun executeGoalAsync(exercise: Exercise, workspace: Path, vararg goal: String): MavenExecutionHandle {
        val callable = Callable { executeGoal(exercise, workspace, LocalLogOutputHandler(exercise, true), *goal) }
        val futureTask = FutureTask(callable)
        val thread = Thread(futureTask)
        thread.start()

        return MavenExecutionHandle(futureTask, thread)
    }

    @JvmStatic
    @JvmOverloads
    fun executeGoal(
        exercise: Exercise,
        workspace: Path,
        logInit: Boolean = false,
        vararg goal: String
    ): MavenInvocationResult = executeGoal(exercise, workspace, LocalLogOutputHandler(exercise, logInit), *goal)

    @JvmStatic
    fun executeGoal(
        exercise: Exercise,
        workspace: Path,
        outputHandler: LocalLogOutputHandler = LocalLogOutputHandler(exercise),
        vararg goal: String
    ): MavenInvocationResult {
        MavenResolver.declareMavenHomeIfNeeded()
        val localRepositoryPath: Path = initializeLocalRepo(workspace)
        val invoker: Invoker = CustomerInvoker()
        invoker.localRepositoryDirectory = localRepositoryPath.toFile()
        //val outputHandler = MemoryOutputHandler()
        val invocationRequest = DefaultInvocationRequest()
            .setPomFile(MavenReader.pomFilePath(exercise).toFile())
            .setBatchMode(true)
            .setGoals(goal.toList())
            .setOutputHandler(outputHandler)
            .setErrorHandler(outputHandler)
        return invoke(
            invoker,
            invocationRequest,
            outputHandler
        )
    }

    private operator fun invoke(
        invoker: Invoker,
        invocationRequest: InvocationRequest,
        logs: LocalLogOutputHandler
    ): MavenInvocationResult {
        return try {
            val javaHome = System.getProperty("java.home")
            invocationRequest.javaHome = File(javaHome)
            val result = invoker.execute(invocationRequest)
            if (result.exitCode != 0) {
                MavenInvocationResult(MavenInvocationResult.Status.KO, logs.oneline())
            } else MavenInvocationResult(MavenInvocationResult.Status.OK, logs.oneline())
        } catch (e: MavenInvocationException) {
            MavenInvocationResult(MavenInvocationResult.Status.KO, logs.oneline())
        }
    }

    private fun initializeLocalRepo(workspace: Path): Path {
        val localRepositoryPath = workspace.resolve(".m2")
        if (Files.exists(localRepositoryPath) && !Files.isDirectory(localRepositoryPath)) {
            try {
                Files.delete(localRepositoryPath)
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }
        }
        if (!Files.exists(localRepositoryPath)) {
            try {
                Files.createDirectory(localRepositoryPath)
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }
        }
        return localRepositoryPath
    }
}

class MavenInvocationResult(val status: Status, val output: String) {
    enum class Status {
        OK, KO
    }
}

class LocalLogOutputHandler(val exercise: Exercise, val logInit: Boolean = false) : InvocationOutputHandler {
    private val logger = LoggerFactory.getLogger(LocalLogOutputHandler::class.java)

    private val hostPath = exercise.root.toAbsolutePath().parent.parent.toString()
    private val hostPathEscaped = hostPath.replace(File.separator, "\\\\")
    private val hostPathUnix = hostPath.replace(File.separator, "/")

    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HH_mm_ss")
    private val logPath = exercise.root.parent.resolve("maven-" + LocalDateTime.now().format(formatter) + ".log")

    @Volatile
    private var lines: MutableList<String> = ArrayList()

    init {
        if (logInit) {
            logger.info("Starting Maven process > $logPath")
        }
        if (!Files.exists(logPath)) {
            Files.createFile(logPath)
        }
    }

    override fun consumeLine(line: String) {
        val sanitizedLine = line
            .replace(hostPath, "")
            .replace(hostPathEscaped, "")
            .replace(hostPathUnix, "")
        lines.add(sanitizedLine)
        logPath.toFile().appendText(sanitizedLine + "\n")
    }

    fun oneline() = lines.joinToString("\n")
}


