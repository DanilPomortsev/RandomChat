package org.random.chat.common.dbqueue

import ru.yoomoney.tech.dbqueue.api.QueueProducer

class QueueProducers(
    private val producers: Map<String, QueueProducer<String>>,
) {
    fun get(queueName: String): QueueProducer<String> {
        return producers[queueName]
            ?: error("No queue producer registered for queue $queueName")
    }
}
