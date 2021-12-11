package com.github.lernejo.korekto.toolkit.misc

import org.mozilla.universalchardet.UniversalDetector
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class InteractiveProcess(
    val process: Process,
    private val processReadTimeout: Long = System.getProperty("PROCESS_READ_TIMEOUT", "400").toLong(),
    private val processReadRetryDelay: Long = System.getProperty("PROCESS_READ_RETRY_DELAY", "50").toLong()
) : AutoCloseable {

    private val logger = LoggerFactory.getLogger(InteractiveProcess::class.java)

    override fun close() {
        process.destroyForcibly()
    }

    fun read() = readStream(process.inputStream)
    fun readErr() = readStream(process.errorStream)

    fun write(s: String) {
        try {
            val writer = BufferedWriter(OutputStreamWriter(process.outputStream))
            writer.write(s)
            writer.flush()
            TimeUnit.MILLISECONDS.sleep(100L)
        } catch (e: IOException) {
            logger.warn("Unable to write to process input: " + e.message)
        } catch (e: InterruptedException) {
            logger.warn("Unable to write to process input: " + e.message)
        }
    }

    private fun readAll(): String {
        val clientLog = java.lang.StringBuilder()
        do {
            val line = read()
            if (line != null && line.isNotEmpty()) {
                clientLog.append(line).append("\n");
            } else {
                break
            }
        } while (true)
        return clientLog.trim().toString()
    }

    private fun readStream(inputStream: InputStream): String? {
        val start = System.currentTimeMillis()
        do {
            return try {
                TimeUnit.MILLISECONDS.sleep(processReadRetryDelay)
                val sb = StringBuilder()
                while (inputStream.available() > 0) {
                    val bytes = inputStream.readNBytes(inputStream.available())
                    val detector = UniversalDetector()
                    detector.handleData(bytes)
                    detector.dataEnd()
                    val detectedCharset = detector.detectedCharset
                    sb.append(
                        String(
                            bytes,
                            if (detectedCharset != null) Charset.forName(detectedCharset) else StandardCharsets.UTF_8
                        )
                    )
                }
                val lineOutput = sb.toString().trim()
                if (lineOutput.isEmpty()) {
                    continue
                }
                lineOutput
            } catch (e: IOException) {
                logger.warn("Unable to read process output: " + e.message)
                null
            } catch (e: InterruptedException) {
                logger.warn("Unable to read process output: " + e.message)
                null
            }
        } while (System.currentTimeMillis() - start < processReadTimeout)
        logger.debug("No process output to read in $processReadTimeout ms")
        return null
    }
}
