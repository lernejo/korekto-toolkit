package com.github.lernejo.korekto.toolkit.thirdparty.maven.invoker

import org.apache.maven.shared.invoker.InvocationResult
import org.apache.maven.shared.utils.cli.CommandLineException

class DefaultInvocationResult
/**
 * Creates a new invocation result
 */
internal constructor() : InvocationResult {
    /**
     * The exception that prevented to execute the command line, will be `null` if Maven could be
     * successfully started.
     */
    private var executionException: CommandLineException? = null

    /**
     * The exit code reported by the Maven invocation.
     */
    private var exitCode = Int.MIN_VALUE

    /**
     *
     * Getter for the field `exitCode`.
     *
     * @return a int.
     */
    override fun getExitCode(): Int {
        return exitCode
    }

    /**
     *
     * Getter for the field `executionException`.
     *
     * @return a [org.apache.maven.shared.utils.cli.CommandLineException] object.
     */
    override fun getExecutionException(): CommandLineException {
        return executionException!!
    }

    /**
     * Sets the exit code reported by the Maven invocation.
     *
     * @param exitCode The exit code reported by the Maven invocation.
     */
    fun setExitCode(exitCode: Int) {
        this.exitCode = exitCode
    }

    /**
     * Sets the exception that prevented to execute the command line.
     *
     * @param executionException The exception that prevented to execute the command line, may be `null`.
     */
    fun setExecutionException(executionException: CommandLineException?) {
        this.executionException = executionException
    }
}
