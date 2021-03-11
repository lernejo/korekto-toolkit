package com.github.lernejo.korekto.toolkit.thirdparty.maven

import com.github.lernejo.korekto.toolkit.Exercise
import org.apache.maven.shared.invoker.*
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors

object MavenExecutor {

    @JvmStatic
    fun executeGoal(exercise: Exercise, workspace: Path, vararg goal: String): MavenInvocationResult {
        val localRepositoryPath: Path = initializeLocalRepo(workspace)
        val invoker: Invoker = DefaultInvoker()
        invoker.localRepositoryDirectory = localRepositoryPath.toFile()
        val outputHandler = MemoryOutputHandler()
        invoker.setOutputHandler(outputHandler)
        val invocationRequest = DefaultInvocationRequest()
            .setPomFile(pomFilePath(exercise).toFile())
            .setBatchMode(true)
            .setGoals(goal.toList())
            .setOutputHandler(outputHandler)
            .setErrorHandler(outputHandler)
        return invoke(
            exercise,
            invoker,
            invocationRequest,
            outputHandler
        )
    }

    private operator fun invoke(
        exercise: Exercise,
        invoker: Invoker,
        invocationRequest: InvocationRequest,
        logs: MemoryOutputHandler
    ): MavenInvocationResult {
        return try {
            val javaHome = System.getProperty("java.home")
            invocationRequest.javaHome = File(javaHome)
            val result = invoker.execute(invocationRequest)
            if (result.exitCode != 0) {
                MavenInvocationResult(MavenInvocationResult.Status.KO, logs.oneline(exercise))
            } else MavenInvocationResult(MavenInvocationResult.Status.OK, logs.oneline(exercise))
        } catch (e: MavenInvocationException) {
            MavenInvocationResult(MavenInvocationResult.Status.KO, logs.oneline(exercise))
        }
    }

    private fun pomFilePath(exercise: Exercise): Path {
        return exercise.root.resolve("pom.xml")
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

internal class MemoryOutputHandler : InvocationOutputHandler {
    val lines: MutableList<String> = ArrayList()
    override fun consumeLine(line: String) {
        lines.add(line)
    }

    fun oneline(exercise: Exercise): String {
        val path1 = exercise.root.toAbsolutePath().parent.parent.toString()
        val path2 = path1.replace(File.separator, "\\\\")
        val path3 = path1.replace(File.separator, "/")
        return lines.stream()
            .skip(3)
            .map { s: String ->
                s.replace(
                    path1,
                    ""
                )
            }
            .map { s: String ->
                s.replace(
                    path2,
                    ""
                )
            }
            .map { s: String ->
                s.replace(
                    path3,
                    ""
                )
            }
            .collect(Collectors.joining("\n"))
    }
}


