package com.github.lernejo.korekto.toolkit.misc

import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*

object Processes {
    @JvmStatic
    @JvmOverloads
    fun launch(command: String, workingDirectory: Path? = null): ProcessResult {
        val builder = ProcessBuilder()
        if (OS.WINDOWS.isCurrentOs) {
            builder.command("cmd.exe", "/c", command)
        } else {
            builder.command("sh", "-c", command)
        }
        if (workingDirectory != null) {
            builder.directory(workingDirectory.toFile())
        }
        builder.environment()["JAVA_HOME"] = System.getProperty("java.home")
        return try {
            val process = builder.start()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val stderr = readStream(process.errorStream)
                return ProcessResult.error(exitCode, stderr)
            }
            val stdout = readStream(process.inputStream)
            ProcessResult.success(stdout)
        } catch (e: IOException) {
            ProcessResult.error(e)
        } catch (e: InterruptedException) {
            ProcessResult.error(e)
        }
    }

    private fun readStream(inputStream: InputStream): String {
        BufferedInputStream(inputStream).use {
            it.mark(1)
            val firstByte = it.read()
            return if (firstByte != -1) {
                it.reset()
                Scanner(it, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next()
            } else {
                ""
            }
        }
    }

    class ProcessResult(val cause: Exception?, val exitCode: Int, val output: String?) {
        companion object {
            fun error(cause: Exception?): ProcessResult {
                return ProcessResult(cause, -1, null)
            }

            fun error(exitCode: Int, stderr: String): ProcessResult {
                return ProcessResult(null, exitCode, stderr)
            }

            fun success(output: String): ProcessResult {
                return ProcessResult(null, 0, output)
            }
        }
    }
}
