package com.github.lernejo.korekto.toolkit.thirdparty.git

import com.github.lernejo.korekto.toolkit.WarningException
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.InvalidRemoteException
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
    private val URI_WITHOUT_CRED_PATTERN = Pattern.compile("(?<protocol>https?://)(?<hostAndMore>.+)")

    data class Creds(val uriWithoutCred: String, val username: String?, val password: String?)

    fun extractCredParts(uri: String): Creds {
        val uriCredMatcher = URI_WITH_CRED_PATTERN.matcher(uri)
        val credPresent = uriCredMatcher.matches()

        val (hostAndMore, uriWithoutCred) = if (credPresent) {
            val ham = uriCredMatcher.group("hostAndMore")
            Pair(ham, uriCredMatcher.group("protocol") + ham)
        } else {
            Pair("", uri)
        }

        if (credPresent) {
            val parts = uriCredMatcher.group("cred").split(":")
            if (parts.size == 1) {
                val token = parts[0]
                if (hostAndMore.startsWith("github.com")) {
                    System.setProperty("github_token", token)
                }
                return Creds(uriWithoutCred, token, "")
            } else {
                return Creds(uriWithoutCred, parts[0], parts[1])
            }
        } else {
            return Creds(uriWithoutCred, null, null)
        }
    }

    private fun insertToken(uri: String, token: String): String {
        val uriCredMatcher = URI_WITH_CRED_PATTERN.matcher(uri)
        return if (uriCredMatcher.matches()) {
            uri
        } else {
            val uriMatcher = URI_WITHOUT_CRED_PATTERN.matcher(uri)
            if (uriMatcher.matches()) {
                uriMatcher.group("protocol") + "x-access-token:" + token + "@" + uriMatcher.group("hostAndMore")
            } else {
                uri
            }
        }
    }

    @JvmStatic
    fun clone(uri: String, path: Path): Git {
        val creds = extractCredParts(uri)

        return try {
            val cloneCommand = Git.cloneRepository()
                .setURI(uri)
                .setDirectory(path.toFile())
            val token = System.getProperty("github_token")
            if (creds.username != null) {
                cloneCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(creds.username, creds.password))
            } else if (token != null) {
                cloneCommand
                    .setURI(insertToken(uri, token))
                    .setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
            }
            val git = cloneCommand
                .setCloneAllBranches(true)
                .call()
            LOGGER.debug("Cloning in: " + git.repository.directory)
            git
        } catch(e: InvalidRemoteException) {
            throw WarningException("Unable to clone in ${path.toAbsolutePath()}: Missing or inaccessible repository", e)
        } catch (e: GitAPIException) {
            throw buildWarningException(path, e)
        } catch (e: JGitInternalException) {
            throw buildWarningException(path, e)
        }
    }

    private fun buildWarningException(
        path: Path,
        e: Exception
    ) = WarningException("Unable to clone in ${path.toAbsolutePath()}: ${e.message}", e)

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

    internal fun extractCreds(uri: String): Creds {
        val token = System.getProperty("github_token")
        return if (token != null) {
            Creds(uri, token, "")
        } else {
            extractCredParts(uri)
        }
    }

    @JvmStatic
    fun forcePull(git: Git, uri: String) {
        val creds = extractCreds(uri)

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
        git.reset().setMode(ResetCommand.ResetType.HARD).call()
        git.clean().setCleanDirectories(true).setForce(true).call()
        git.pull().call()
    }
}
