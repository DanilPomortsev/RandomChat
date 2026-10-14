package org.random.chat.common.dbqueue

import com.fasterxml.jackson.databind.ObjectMapper
import ru.yoomoney.tech.dbqueue.api.TaskPayloadTransformer

class QueuePayloadTransformer<T : QueuePayload>(
    private val type: Class<T>,
    private val objectMapper: ObjectMapper,
) : TaskPayloadTransformer<T> {
    override fun toObject(payload: String?): T? {
        if (payload == null) {
            return null
        }
        return objectMapper.readValue(payload, type)
    }

    override fun fromObject(payload: T?): String? {
        if (payload == null) {
            return null
        }
        return objectMapper.writeValueAsString(payload)
    }
}
