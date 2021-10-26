package com.github.lernejo.korekto.toolkit.misc

import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class PortsTest {

    @Test
    internal fun `Port listened then freed scenario`() {
        val start = System.currentTimeMillis()
        val threadPool = Executors.newFixedThreadPool(1)
        val port = ServerSocket(0).use { it.localPort }
        var serverHandle: ServerSocket? = null
        threadPool.submit {
            TimeUnit.MILLISECONDS.sleep(100L)
            serverHandle = ServerSocket(port)
            println("[${System.currentTimeMillis() - start}] Server started")
        }
        Ports.waitForPortToBeListenedTo(port, TimeUnit.MILLISECONDS, 500L)
        println("[${System.currentTimeMillis() - start}] Port detected listened to")
        threadPool.submit {
            TimeUnit.MILLISECONDS.sleep(100L)
            serverHandle?.close()
            println("[${System.currentTimeMillis() - start}] Server stopped")
        }
        Ports.waitForPortToBeFreed(port, TimeUnit.MILLISECONDS, 500L)
        println("[${System.currentTimeMillis() - start}] Port detected freed")
    }
}
