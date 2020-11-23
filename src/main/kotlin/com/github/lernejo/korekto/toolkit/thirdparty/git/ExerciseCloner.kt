package com.github.lernejo.korekto.toolkit.thirdparty.git

import com.github.lernejo.korekto.toolkit.Exercise
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitRepository.clone
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitRepository.forcePull
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitRepository.here
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class ExerciseCloner(private val workspace: Path) {

    fun gitClone(uri: String): Exercise {
        return gitClone(workspace, uri)
    }

    companion object {
        fun toGithubSsh(slug: String): String  = "git@github.com:$slug.git"
        fun toGitHubHttps(slug: String): String = "https://github.com/$slug.git"

        fun extractSlug(url: String): String {
            var name = url
            val dotCom = name.lastIndexOf(".com")
            if (dotCom > -1) {
                name = name.substring(dotCom + 5)
            }
            val dotIndex = name.lastIndexOf('.')
            if (dotIndex > -1) {
                name = name.substring(0, dotIndex)
            }
            return name
        }

        fun gitClone(workspace: Path, uri: String): Exercise {
            val name = extractSlug(uri)
            val path = Paths.get(workspace.toString() + File.separator + name).toAbsolutePath()
            val potentialRepo = here(path)
            if (potentialRepo.isPresent) {
                try {
                    forcePull(potentialRepo.get())
                } catch (e: RuntimeException) {
                    throw RuntimeException("Could not pull -f repository: $path ($uri)", e)
                }
            } else {
                clone(uri, path)
            }
            return Exercise(name, path)
        }
    }

}
