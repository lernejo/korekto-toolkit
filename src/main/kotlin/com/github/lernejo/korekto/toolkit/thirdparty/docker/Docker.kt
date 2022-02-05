package com.github.lernejo.korekto.toolkit.thirdparty.docker

import com.github.lernejo.korekto.toolkit.misc.Ports
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import java.util.concurrent.TimeUnit

class MappedPortsContainer @JvmOverloads constructor(
    imageName: String,
    private val containerServicePort: Int,
    private val upMessageBuilder: (Int, List<Int>) -> String = { sp: Int, sps: List<Int> -> "$imageName up and serving $sp ($sps)" },
    private vararg val containerSecondaryPorts: Int,
    private val upTimeoutInMs: Long = System.getProperty("DOCKER_UP_TIMEOUT", "2000").toLong()
) : GenericContainer<MappedPortsContainer>(imageName) {

    private val logger = LoggerFactory.getLogger(MappedPortsContainer::class.java)

    init {
        addExposedPort(containerServicePort)
        containerSecondaryPorts.forEach { addExposedPort(it) }
    }

    private var up = false

    val servicePort by lazy { getMappedPort(containerServicePort)!! }

    val secondaryPorts by lazy { containerSecondaryPorts.map { this.getMappedPort(it)!! }.toList() }

    fun startAndWaitForServiceToBeUp(): MappedPortsContainer {
        if (!up) {
            try {
                start()
            } catch (e: RuntimeException) {
                throw IllegalStateException("Unable to use Docker, make sure the Docker engine is started", e)
            }
            Ports.waitForPortToBeListenedTo(servicePort, TimeUnit.MILLISECONDS, upTimeoutInMs)
            logger.info(upMessageBuilder(servicePort, secondaryPorts))
            up = true
        }
        return this
    }
}
