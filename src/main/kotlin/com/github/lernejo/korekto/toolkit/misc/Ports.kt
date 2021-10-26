package com.github.lernejo.korekto.toolkit.misc

import java.io.IOException
import java.net.Socket
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

object Ports {
    @JvmStatic
    fun waitForPortToBeListenedTo(port: Int, timeoutUnit: TimeUnit, timeout: Long) {
        waitForPortToBe(port, timeoutUnit, timeout) { p -> isListened(p) }
    }

    @JvmStatic
    fun waitForPortToBeFreed(port: Int, timeoutUnit: TimeUnit, timeout: Long) {
        waitForPortToBe(port, timeoutUnit, timeout) { p -> !isListened(p) }
    }

    private fun waitForPortToBe(port: Int, timeoutUnit: TimeUnit, timeout: Long, condition: (Int) -> Boolean) {
        val timeoutInMs = TimeUnit.MILLISECONDS.convert(timeout, timeoutUnit)
        val startTime = System.currentTimeMillis()
        do {
            if (condition(port)) {
                return
            }
            try {
                TimeUnit.MILLISECONDS.sleep(50L)
            } catch (e: InterruptedException) {
                throw IllegalStateException(e)
            }
        } while (System.currentTimeMillis() - startTime < timeoutInMs)
        throw CancellationException()
    }

    @JvmStatic
    fun isListened(port: Int): Boolean {
        try {
            Socket(null as String?, port).use { return true }
        } catch (ignored: IOException) {
            return false
        }
    }
}
