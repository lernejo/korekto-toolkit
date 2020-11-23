package com.github.lernejo.korekto.toolkit.thirdparty.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.JGitInternalException
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Path
import java.util.*

object GitRepository {
    private val LOGGER = LoggerFactory.getLogger(GitRepository::class.java)

    @JvmStatic
    fun clone(uri: String?, path: Path): Git {
        return try {
            val git = Git.cloneRepository()
                .setURI(uri)
                .setDirectory(path.toFile())
                .call()
            LOGGER.debug("Cloning in: " + git.repository.directory)
            git
        } catch (e: GitAPIException) {
            throw RuntimeException("Unable to clone in " + path.toAbsolutePath(), e)
        } catch (e: JGitInternalException) {
            throw RuntimeException("Unable to clone in " + path.toAbsolutePath(), e)
        }
    }

    @JvmStatic
    fun here(path: Path): Optional<Git> {
        return try {
            val repository = FileRepositoryBuilder().setGitDir(path.resolve(".git").toFile())
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()
            val commits = Git(repository).log().all().call()
            if (commits.iterator().hasNext()) {
                Optional.of(Git(repository))
            } else {
                Optional.empty()
            }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        } catch (e: NoHeadException) {
            Optional.empty()
        } catch (e: GitAPIException) {
            throw RuntimeException(e)
        }
    }

    @JvmStatic
    fun forcePull(git: Git) {
        try {
            git.fetch()
                .setForceUpdate(true)
                .call()
            git.reset()
                .setMode(ResetCommand.ResetType.HARD)
                .setRef("origin/master")
                .call()
        } catch (e: GitAPIException) {
            throw RuntimeException(e)
        }
    }
}
