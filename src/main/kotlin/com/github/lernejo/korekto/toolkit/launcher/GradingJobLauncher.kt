package com.github.lernejo.korekto.toolkit.launcher

import com.github.lernejo.korekto.toolkit.Grader
import com.github.lernejo.korekto.toolkit.GradingConfiguration
import com.github.lernejo.korekto.toolkit.GradingJob
import com.github.lernejo.korekto.toolkit.misc.Loader
import com.github.lernejo.korekto.toolkit.misc.OS
import com.github.lernejo.korekto.toolkit.misc.Processes
import picocli.CommandLine
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

@CommandLine.Command(
    name = "grading-job",
    mixinStandardHelpOptions = true,
    description = ["Launch a Grading Job."]
)
class GradingJobLauncher : Callable<Int> {

    @CommandLine.Option(names = ["-g", "--group"], description = ["grade several exercices based on a slug file"])
    var group: Boolean = false

    @CommandLine.Option(
        names = ["-fp", "--force-pull"],
        description = ["force pull exercise, removing local modifications if any"],
        negatable = true
    )
    var forcePull: Boolean = true;

    @CommandLine.Option(names = ["-f", "--slugFile"], description = ["slug file used in group mode"])
    var slugFile: Path = Paths.get("slugs.txt")

    @CommandLine.Option(names = ["-s", "--slug"], description = ["slug to grade exercise from"])
    var slug: Optional<String> = Optional.empty()

    override fun call(): Int {
        val grader = Grader.load() ?: throw IllegalArgumentException("No Grader implementation declared")
        val resetWorkspace = grader.needsWorkspaceReset()

        return when {
            group -> {
                val slugs = Loader.loadLines(slugFile.toAbsolutePath())
                val dryRun = System.getProperty("dryRun", "false").toBoolean()
                    && System.getProperty("github_token") != null
                val gradingJob = buildGroupGradingJob(grader, AtomicInteger(), dryRun)
                gradingJob.runBatch(slugs, grader::slugToRepoUrl, resetWorkspace = resetWorkspace)
                0
            }
            slug.isPresent -> {
                val repoUrl = grader.slugToRepoUrl(slug.get())
                val configuration = GradingConfiguration(repoUrl, "", "")
                if (resetWorkspace) {
                    OS.CURRENT_OS?.deleteDirectoryCommand(configuration.workspace)
                        ?.let { Processes.launch(it, null) }
                }
                buildLocalGradingJob(grader).run(configuration)
            }
            else -> {
                buildContainerizedGradingJob(grader).run()
            }
        }
    }

    private fun buildGroupGradingJob(grader: Grader, counter: AtomicInteger, dryRun: Boolean) = GradingJob()
        .addCloneStep(forcePull)
        .addStep("display") { _, _ -> println(counter.incrementAndGet()) }
        .addStep("grading", grader)
        .addStoreResultsLocallyStep()
        .addUpsertGitHubIssuesStep(Locale.FRENCH, grader::deadline, dryRun)

    private fun buildLocalGradingJob(grader: Grader) = GradingJob()
        .addCloneStep(forcePull)
        .addStep("grading", grader)
        .addStoreResultsLocallyStep()
        .addUpsertGitHubIssuesStep(Locale.FRENCH, grader::deadline, dryRun = true)

    private fun buildContainerizedGradingJob(grader: Grader) = GradingJob()
        .addCloneStep(forcePull)
        .addStep("grading", grader)
        .addUpsertGitHubIssuesStep(Locale.FRENCH, grader::deadline)
        .addSendStep()

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            exitProcess(CommandLine(GradingJobLauncher()).execute(*args))
        }
    }
}
