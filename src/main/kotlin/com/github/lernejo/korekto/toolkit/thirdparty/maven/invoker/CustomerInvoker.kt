package com.github.lernejo.korekto.toolkit.thirdparty.maven.invoker

import org.apache.maven.shared.invoker.*
import org.apache.maven.shared.utils.cli.*
import java.io.InputStream
import java.nio.charset.Charset

/**
 * This is a mere copy of {@link DefaultInvoker} but handling SIGINT by destroying launched process and all its children.
 */
class CustomerInvoker : DefaultInvoker() {

    @Throws(MavenInvocationException::class)
    override fun execute(request: InvocationRequest): InvocationResult? {
        val cliBuilder = MavenCommandLineBuilder()
        val logger = logger
        if (logger != null) {
            cliBuilder.logger = getLogger()
        }
        val localRepo = localRepositoryDirectory
        if (localRepo != null) {
            cliBuilder.localRepositoryDirectory = localRepositoryDirectory
        }
        val mavenHome = mavenHome
        if (mavenHome != null) {
            cliBuilder.mavenHome = getMavenHome()
        }
        val mavenExecutable = mavenExecutable
        if (mavenExecutable != null) {
            cliBuilder.mavenExecutable = mavenExecutable
        }
        val workingDirectory = workingDirectory
        if (workingDirectory != null) {
            cliBuilder.workingDirectory = getWorkingDirectory()
        }
        val cli: Commandline = try {
            cliBuilder.build(request)
        } catch (e: CommandLineConfigurationException) {
            throw MavenInvocationException("Error configuring command-line. Reason: " + e.message, e)
        }
        val result = DefaultInvocationResult()
        try {
            val exitCode = executeCommandLine(cli, request, request.timeoutInSeconds)
            result.exitCode = exitCode
        } catch (e: CommandLineException) {
            result.setExecutionException(e)
        }
        return result
    }

    @Throws(CommandLineException::class)
    private fun executeCommandLine(cli: Commandline, request: InvocationRequest, timeoutInSeconds: Int): Int {
        val result: Int
        val inputStream = request.getInputStream(null)
        val outputHandler = request.getOutputHandler(SystemOutHandler())
        val errorHandler = request.getErrorHandler(SystemOutHandler())
        if (logger.isDebugEnabled) {
            logger.debug("Executing: $cli")
        }
        result = if (request.isBatchMode) {
            if (inputStream != null) {
                logger.info("Executing in batch mode. The configured input stream will be ignored.")
            }
            executeCommandLine(cli, outputHandler, errorHandler, timeoutInSeconds)
        } else {
            if (inputStream == null) {
                logger.warn(
                    "Maven will be executed in interactive mode"
                        + ", but no input stream has been configured for this MavenInvoker instance."
                )
                executeCommandLine(cli, outputHandler, errorHandler, timeoutInSeconds)
            } else {
                executeCommandLine(
                    cli, inputStream, outputHandler, errorHandler,
                    timeoutInSeconds
                )
            }
        }
        return result
    }

    private fun executeCommandLine(
        cli: Commandline,
        systemOut: StreamConsumer,
        systemErr: StreamConsumer,
        timeoutInSeconds: Int
    ): Int {
        return executeCommandLine(cli, null, systemOut, systemErr, timeoutInSeconds)
    }

    private fun executeCommandLine(
        cli: Commandline,
        systemIn: InputStream?,
        systemOut: StreamConsumer,
        systemErr: StreamConsumer,
        timeoutInSeconds: Int
    ): Int {
        val future = executeCommandLineAsCallable(
            cli, systemIn, systemOut, systemErr, timeoutInSeconds,
            null, null
        )
        return future.call()
    }

    private val NANOS_PER_SECOND = 1000000000L
    private val MILLIS_PER_SECOND = 1000L

    private fun isAlive(p: Process?): Boolean {
        return if (p == null) {
            false
        } else try {
            p.exitValue()
            false
        } catch (e: IllegalThreadStateException) {
            true
        }
    }

    @Throws(CommandLineException::class)
    fun executeCommandLineAsCallable(
        cl: Commandline?,
        systemIn: InputStream?,
        systemOut: StreamConsumer?,
        systemErr: StreamConsumer?,
        timeoutInSeconds: Int,
        runAfterProcessTermination: Runnable?,
        streamCharset: Charset?
    ): CommandLineCallable {
        requireNotNull(cl) { "cl cannot be null." }
        val p = cl.execute()
        val processHook: Thread = object : Thread() {
            override fun run() {
                if (isAlive(p)) {
                    destroyProcessWithChildren(p.toHandle()!!)
                }
            }

            init {
                name = "CommandLineUtils process shutdown hook"
                contextClassLoader = null
            }
        }
        ShutdownHookUtils.addShutDownHook(processHook)
        return CommandLineCallable {
            var inputFeeder: StreamFeeder? = null
            var outputPumper: StreamPumper? = null
            var errorPumper: StreamPumper? = null
            try {
                if (systemIn != null) {
                    inputFeeder = StreamFeeder(systemIn, p.outputStream)
                    inputFeeder.setName("StreamFeeder-systemIn")
                    inputFeeder.start()
                }
                outputPumper = StreamPumper(p.inputStream, systemOut)
                outputPumper.name = "StreamPumper-systemOut"
                outputPumper.start()
                errorPumper = StreamPumper(p.errorStream, systemErr)
                errorPumper.name = "StreamPumper-systemErr"
                errorPumper.start()
                val returnValue: Int = if (timeoutInSeconds <= 0) {
                    p.waitFor()
                } else {
                    val now = System.nanoTime()
                    val timeout = now + NANOS_PER_SECOND * timeoutInSeconds
                    while (isAlive(p) && System.nanoTime() < timeout) {
                        // The timeout is specified in seconds. Therefore we must not sleep longer than one second
                        // but we should sleep as long as possible to reduce the number of iterations performed.
                        Thread.sleep(MILLIS_PER_SECOND - 1L)
                    }
                    if (isAlive(p)) {
                        throw InterruptedException(
                            String.format(
                                "Process timed out after %d seconds.",
                                timeoutInSeconds
                            )
                        )
                    }
                    p.exitValue()
                }

// TODO Find out if waitUntilDone needs to be called using a try-finally construct. The method may throw an
//      InterruptedException so that calls to waitUntilDone may be skipped.
//                    try
//                    {
//                        if ( inputFeeder != null )
//                        {
//                            inputFeeder.waitUntilDone();
//                        }
//                    }
//                    finally
//                    {
//                        try
//                        {
//                            outputPumper.waitUntilDone();
//                        }
//                        finally
//                        {
//                            errorPumper.waitUntilDone();
//                        }
//                    }
                inputFeeder?.waitUntilDone()
                outputPumper.waitUntilDone()
                errorPumper.waitUntilDone()
                if (inputFeeder != null) {
                    inputFeeder.close()
                    if (inputFeeder.exception != null) {
                        throw CommandLineException("Failure processing stdin.", inputFeeder.exception)
                    }
                }
                if (outputPumper.exception != null) {
                    throw CommandLineException("Failure processing stdout.", outputPumper.exception)
                }
                if (errorPumper.exception != null) {
                    throw CommandLineException("Failure processing stderr.", errorPumper.exception)
                }
                return@CommandLineCallable returnValue
            } catch (ex: InterruptedException) {
                if (isAlive(p)) {
                    destroyProcessWithChildren(p.toHandle()!!)
                }
                throw CommandLineTimeOutException(
                    "Error while executing external command, process killed.",
                    ex
                )
            } finally {
                inputFeeder?.disable()
                outputPumper?.disable()
                errorPumper?.disable()
                try {
                    runAfterProcessTermination?.run()
                } finally {
                    ShutdownHookUtils.removeShutdownHook(processHook)
                    try {
                        processHook.run()
                    } finally {
                        inputFeeder?.close()
                    }
                }
            }
        }
    }

    private fun destroyProcessWithChildren(p: ProcessHandle) {
        p.children().forEach { c -> destroyProcessWithChildren(c) }
        p.destroy()
    }
}
