package com.github.lernejo.korekto.toolkit.partgrader

import com.github.lernejo.korekto.toolkit.GradePart
import com.github.lernejo.korekto.toolkit.GradingContext
import com.github.lernejo.korekto.toolkit.PartGrader
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenExecutor
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenInvocationResult
import java.nio.file.Files
import java.nio.file.Path

interface MavenContext {
    fun markAsCompilationFailed()
    fun markAsTestFailed()

    fun hasCompilationFailed(): Boolean
    fun hasTestFailed(): Boolean
}


open class MavenCompileAndTestPartGrader<T>(
    private val name: String,
    private val maxGrade: Double
) : PartGrader<T> where T : GradingContext, T : MavenContext {

    override fun name() = name
    override fun maxGrade() = maxGrade

    open fun beforeCompile(context: T, root: Path) {}
    open fun afterCompile(context: T, root: Path) {}

    override fun grade(context: T): GradePart {
        val root = context.exercise?.root!!
        if (!Files.exists(root.resolve("pom.xml"))) {
            context.markAsCompilationFailed()
            context.markAsTestFailed()
            return result(listOf("Not a Maven project"), 0.0)
        }

        beforeCompile(context, root)

        val invocationResult = MavenExecutor.executeGoal(
            context.exercise!!,
            context.configuration.workspace,
            "clean",
            "test-compile"
        )
        return if (invocationResult.status !== MavenInvocationResult.Status.OK) {
            context.markAsCompilationFailed()
            context.markAsTestFailed()
            result(listOf("Compilation failed, see `mvn test-compile`"), 0.0)
        } else {
            afterCompile(context, root)

            // Install project dependencies to be able to execute a module solely even if it depends on another
            MavenExecutor.executeGoal(context.exercise!!, context.configuration.workspace, "install", "-DskipTests")

            val testRun = MavenExecutor.executeGoal(context.exercise!!, context.configuration.workspace, "verify")
            if (testRun.status !== MavenInvocationResult.Status.OK) {
                context.markAsTestFailed()
                result(listOf("There are test failures, see `mvn verify`"), maxGrade() / 2)
            } else {
                result(listOf(), maxGrade())
            }
        }
    }
}
