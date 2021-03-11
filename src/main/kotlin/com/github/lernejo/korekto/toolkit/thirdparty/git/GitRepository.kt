package com.github.lernejo.korekto.toolkit.thirdparty.git

import com.github.lernejo.korekto.toolkit.WarningException
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.JGitInternalException
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Path
import java.util.*
import java.util.regex.Pattern

object GitRepository {
    private val LOGGER = LoggerFactory.getLogger(GitRepository::class.java)

    private val URI_WITH_CRED_PATTERN = Pattern.compile("(?<protocol>https?://)(?<cred>[^@]+)@(?<hostAndMore>.+)")

    data class Creds(val uriWithoutCred: String, val username: String?, val password: String?)

    fun extractCredParts(uri: String): Creds {
        val uriCredMatcher = URI_WITH_CRED_PATTERN.matcher(uri)
        val credPresent = uriCredMatcher.matches()
        val uriWithoutCred = if (credPresent) uriCredMatcher.group("protocol") + uriCredMatcher.group("hostAndMore") else uri

        if (credPresent) {
            val parts = uriCredMatcher.group("cred").split(":")
            if (parts.size == 1) {
                return Creds(uriWithoutCred, parts[0], "")
            } else {
                return Creds(uriWithoutCred, parts[0], parts[1])
            }
        } else {
            return Creds(uriWithoutCred, null, null)
        }
    }

    @JvmStatic
    fun clone(uri: String, path: Path): Git {
        val creds = extractCredParts(uri)

        return try {
            val cloneCommand = Git.cloneRepository()
                .setURI(uri)
                .setDirectory(path.toFile())
            if (creds.username != null) {
                cloneCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(creds.username, creds.password))
            }
            val git = cloneCommand
                .call()
            LOGGER.debug("Cloning in: " + git.repository.directory)
            git
        } catch (e: GitAPIException) {
            throw WarningException("Unable to clone in " + path.toAbsolutePath(), e)
        } catch (e: JGitInternalException) {
            throw WarningException("Unable to clone in " + path.toAbsolutePath(), e)
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
    fun forcePull(git: Git, uri: String) {
        val creds = extractCredParts(uri)

        try {
            try {
                forcePull(git, creds, "origin/master")
            } catch (e: JGitInternalException) {
                forcePull(git, creds, "origin/main")
            }
        } catch (e: GitAPIException) {
            throw RuntimeException(e)
        }
    }

    fun forcePull(git: Git, creds: Creds, ref: String) {
        val fetchCommand = git.fetch()
            .setForceUpdate(true)
        if (creds.username != null) {
            fetchCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(creds.username, creds.password))
        }
        fetchCommand
            .call()
        git.reset()
            .setMode(ResetCommand.ResetType.HARD)
            .setRef(ref)
            .call()
    }
}
