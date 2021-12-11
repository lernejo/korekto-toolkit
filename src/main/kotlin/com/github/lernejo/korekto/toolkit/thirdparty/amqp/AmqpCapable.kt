package com.github.lernejo.korekto.toolkit.thirdparty.amqp

import com.rabbitmq.client.ConnectionFactory
import java.io.IOException
import java.util.concurrent.TimeoutException

interface AmqpCapable {

    fun deleteQueue(factory: ConnectionFactory, queueName: String?) {
        try {
            factory.newConnection().use {
                it.createChannel().use {
                    it.queueDelete(queueName)
                }
            }
        } catch (e: IOException) {
            throw IllegalStateException("Could not connect to RabbitMQ at " + factory.host + ":" + factory.port, e)
        } catch (e: TimeoutException) {
            throw IllegalStateException("Could not connect to RabbitMQ at " + factory.host + ":" + factory.port, e)
        }
    }
}
