package com.github.lernejo.korekto.toolkit.tests

import com.github.lernejo.korekto.toolkit.Grader
import com.github.lernejo.korekto.toolkit.Grader.Companion.load
import com.github.lernejo.korekto.toolkit.GradingConfiguration
import com.github.lernejo.korekto.toolkit.GradingContext
import com.github.lernejo.korekto.toolkit.GradingJob
import com.github.lernejo.korekto.toolkit.i18n.I18nTemplateResolver
import com.github.lernejo.korekto.toolkit.misc.OS.Companion.CURRENT_OS
import com.github.lernejo.korekto.toolkit.misc.Processes.launch
import com.github.lernejo.korekto.toolkit.misc.RandomSupplier
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitNature
import org.assertj.core.api.Assertions
import org.eclipse.jgit.api.ResetCommand
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.junit.jupiter.params.ParameterizedTest
import java.nio.file.Paths
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

abstract class EndToEndGraderTest {
    @BeforeEach
    open fun setUp() {
        val counter = AtomicInteger()
        GradingContext.randomSource = RandomSupplier { i -> counter.incrementAndGet() % i } // deterministic behavior
    }

    @Test
    fun github_token_is_set() {
        Assertions.assertThat(System.getProperty("github_token"))
            .`as`("github_token system property")
            .isNotBlank
    }

    @ParameterizedTest(name = "(branch={1}) {0}")
    @BranchFileSource
    @EnabledIfSystemProperty(named = "github_token", matches = ".+")
    fun check_project_stages(title: String?, branchName: String, expectedPayload: String?) {
        val grader: Grader<in GradingContext> = load()!!
        val repoUrl = grader.slugToRepoUrl("lernejo")
        val configuration = GradingConfiguration(repoUrl, "", "", workspace)
        val context = execute(branchName, grader, configuration)
        Assertions.assertThat(context)
            .`as`("Grading context")
            .isNotNull
        val result = createIssueContent(context)
        Assertions.assertThat(result)
            .isEqualToIgnoringWhitespace(expectedPayload)
    }

    private fun execute(
        branchName: String,
        grader: Grader<in GradingContext>,
        configuration: GradingConfiguration
    ): GradingContext {
        val contextHolder = AtomicReference<GradingContext>()
        GradingJob()
            .addCloneStep()
            .addStep(
                "switch-branch"
            ) { context: GradingContext ->
                context
                    .exercise!!
                    .lookupNature(GitNature::class.java)
                    .get()
                    .inContextK {
                        it.git.reset().setMode(ResetCommand.ResetType.HARD).call()
                        it.checkout(branchName)
                    }
            }
            .addStep("grading", grader)
            .addStep("report") { context: GradingContext -> contextHolder.set(context) }
            .run(
                configuration,
                { grader.gradingContext(it) as GradingContext })
        return contextHolder.get()
    }

    private fun createIssueContent(context: GradingContext): String {
        val templateContext = HashMap<String, Any?>()
        templateContext["grade"] = context.gradeDetails.grade()
        templateContext["maxGrade"] = context.gradeDetails.maxGrade()
        templateContext["gradeParts"] = context.gradeDetails.parts
        templateContext["deadline"] = null
        templateContext["now"] = Instant.EPOCH
        return I18nTemplateResolver().process("live-issue/body.md", templateContext, Locale.FRENCH).trim { it <= ' ' }
    }

    companion object {
        private val workspace = Paths.get("target/test_repositories").toAbsolutePath()

        @BeforeAll
        @JvmStatic
        open fun setUpAll() {
            launch(CURRENT_OS!!.deleteDirectoryCommand(workspace.resolve("lernejo")), null)
        }
    }
}
