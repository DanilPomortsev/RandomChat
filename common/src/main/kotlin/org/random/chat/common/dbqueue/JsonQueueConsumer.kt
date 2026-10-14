package org.random.chat.common.dbqueue

import com.fasterxml.jackson.databind.ObjectMapper
import java.lang.reflect.ParameterizedType
import ru.yoomoney.tech.dbqueue.api.QueueConsumer
import ru.yoomoney.tech.dbqueue.api.TaskPayloadTransformer
import ru.yoomoney.tech.dbqueue.settings.QueueConfig
import ru.yoomoney.tech.dbqueue.settings.QueueId

abstract class JsonQueueConsumer<T : QueuePayload>(
    queueConfigs: Map<QueueId, QueueConfig>,
    objectMapper: ObjectMapper,
) : QueueConsumer<T> {
    private val payloadType = resolvePayloadType()
    private val queueConfig = queueConfigs.getValue(QueueId(resolveQueueName(payloadType)))
    private val payloadTransformer = QueuePayloadTransformer(payloadType, objectMapper)

    final override fun getQueueConfig(): QueueConfig {
        return queueConfig
    }

    final override fun getPayloadTransformer(): TaskPayloadTransformer<T> {
        return payloadTransformer
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolvePayloadType(): Class<T> {
        val superclass = javaClass.genericSuperclass
        check(superclass is ParameterizedType) {
            "${javaClass.simpleName} must extend JsonQueueConsumer with an explicit payload type"
        }
        return superclass.actualTypeArguments[0] as Class<T>
    }

    private fun resolveQueueName(type: Class<T>): String {
        return runCatching {
            type.getField("QUEUE_NAME").get(null) as String
        }.getOrElse {
            error("${type.simpleName} must declare companion const val QUEUE_NAME")
        }
    }
}
