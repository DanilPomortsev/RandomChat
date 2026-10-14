package org.random.chat.common.dbqueue

import com.fasterxml.jackson.databind.ObjectMapper
import ru.yoomoney.tech.dbqueue.api.EnqueueParams

class DbQueueEnqueueService(
    private val queueProducers: QueueProducers,
    private val objectMapper: ObjectMapper,
) {
    fun enqueue(payload: QueuePayload) {
        queueProducers.get(payload.queueName)
            .enqueue(EnqueueParams.create(objectMapper.writeValueAsString(payload)))
    }
}
