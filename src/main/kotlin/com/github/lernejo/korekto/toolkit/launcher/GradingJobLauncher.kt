package com.github.lernejo.korekto.toolkit.launcher

import com.github.lernejo.korekto.toolkit.Grader
import com.github.lernejo.korekto.toolkit.GradingConfiguration
import com.github.lernejo.korekto.toolkit.GradingContext
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
import kotlin.io.path.absolute
import kotlin.jvm.optionals.getOrNull
import kotlin.system.exitProcess

@CommandLine.Command(
    name = "grading-job",
    mixinStandardHelpOptions = true,
    description = ["Launch a Grading Job."]
)
class GradingJobLauncher : Callable<Int> {

    @CommandLine.Option(names = ["-g", "--group"], description = ["grade several exercises based on a slug file"])
    var group: Boolean = false

    @CommandLine.Option(
        names = ["-fp", "--force-pull"],
        description = ["force pull exercise, removing local modifications if any"],
        negatable = true
    )
    var forcePull: Boolean = true

    @CommandLine.Option(names = ["-f", "--slugFile"], description = ["slug file used in group mode"])
    var slugFile: Path = Paths.get("slugs.txt")

    @CommandLine.Option(names = ["-s", "--slug"], description = ["slug to grade exercise from"])
    var slug: Optional<String> = Optional.empty()

    @CommandLine.Option(names = ["--local-repo"], description = ["Local Git repository, if set, will not clone"])
    var localRepo: Optional<Path> = Optional.empty()

    @CommandLine.Option(names = ["-r", "--report-file"], description = ["Path of the report file to create"])
    var reportFile: Optional<Path> = Optional.empty()

    override fun call(): Int {
        val grader = Grader.load() ?: throw IllegalArgumentException("No Grader implementation declared")
        val resetWorkspace = grader.needsWorkspaceReset()

        return when {
            group -> {
                val slugs = Loader.loadLines(slugFile.toAbsolutePath())
                val dryRun = System.getProperty("dryRun", "true").toBoolean()
                    && System.getProperty("github_token") != null
                val gradingJob = buildGroupGradingJob(grader, AtomicInteger(), dryRun)
                grader.use {
                    gradingJob.runBatch(
                        slugs,
                        grader.name(),
                        grader::slugToRepoUrl,
                        resetWorkspace = resetWorkspace,
                        contextSupplier = grader::gradingContext
                    )
                }
                0
            }
            slug.isPresent -> {
                System.setProperty("github_user", slug.get())
                val repoUrl = grader.slugToRepoUrl(slug.get())
                val configuration = GradingConfiguration(repoUrl, "", "")
                val branch = System.getProperty("git.branch")
                if (resetWorkspace && localRepo.isEmpty) {
                    OS.CURRENT_OS?.deleteDirectoryCommand(configuration.workspace)
                        ?.let { Processes.launch(it, null) }
                }
                grader.use {
                    buildLocalGradingJob(grader, branch, localRepo, reportFile).run(configuration, grader::gradingContext)
                }
            }
            else -> {
                grader.use {
                    buildContainerizedGradingJob(grader).run(contextSupplier = grader::gradingContext)
                }
            }
        }
    }

    private fun buildGroupGradingJob(grader: Grader<GradingContext>, counter: AtomicInteger, dryRun: Boolean) = GradingJob()
        .addCloneStep(forcePull)
        .addStep("display") { _ -> println(counter.incrementAndGet()) }
        .addStep("grading", grader)
        .addStoreResultsLocallyStep()
        .addUpsertGitHubIssuesStep(Locale.FRENCH, grader::deadline, dryRun = true)

    private fun buildLocalGradingJob(grader: Grader<GradingContext>, branch: String?, localRepo: Optional<Path>, reportPath: Optional<Path>) = GradingJob()
        .addCloneStep(forcePull, branch, localRepo.getOrNull()?.absolute())
        .addStep("grading", grader)
        .addStoreResultsLocallyStep(reportPath.getOrNull()?.absolute())
        .addUpsertGitHubIssuesStep(Locale.FRENCH, grader::deadline, dryRun = true)

    private fun buildContainerizedGradingJob(grader: Grader<GradingContext>) = GradingJob()
        .addCloneStep(forcePull)
        .addStep("grading", grader)
        .addUpsertGitHubIssuesStep(Locale.FRENCH, grader::deadline, dryRun = true)
        .addSendStep()

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            exitProcess(CommandLine(GradingJobLauncher()).execute(*args))
        }
    }
}
