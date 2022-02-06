package com.github.lernejo.korekto.toolkit.thirdparty.amqp

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import java.io.IOException
import java.util.concurrent.TimeoutException

interface AmqpCapable {

    fun deleteQueue(factory: ConnectionFactory, queueName: String) {
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

    fun doesQueueExists(factory: ConnectionFactory, queueName: String): Boolean {
        try {
            factory.newConnection().use {
                return doesQueueExists(it, queueName)
            }
        } catch (e: IOException) {
            throw IllegalStateException("Could not connect to RabbitMQ at " + factory.host + ":" + factory.port, e)
        } catch (e: TimeoutException) {
            throw IllegalStateException("Could not connect to RabbitMQ at " + factory.host + ":" + factory.port, e)
        }
    }

    fun doesQueueExists(connection: Connection, queueName: String): Boolean {
        try {
            connection.createChannel().use { channel ->
                val declareOk = channel.queueDeclarePassive(queueName)
                return declareOk != null
            }
        } catch (e: IOException) {
            return false
        }
    }
}
