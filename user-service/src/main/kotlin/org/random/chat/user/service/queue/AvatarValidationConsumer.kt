package org.random.chat.user.service.queue

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.OffsetDateTime
import org.random.chat.common.dbqueue.JsonQueueConsumer
import org.random.chat.user.service.domain.AvatarStatus
import org.random.chat.user.service.repository.AvatarRepository
import org.springframework.stereotype.Component
import ru.yoomoney.tech.dbqueue.api.Task
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult
import ru.yoomoney.tech.dbqueue.settings.QueueConfig
import ru.yoomoney.tech.dbqueue.settings.QueueId

@Component
class AvatarValidationConsumer(
    queueConfigs: Map<QueueId, QueueConfig>,
    objectMapper: ObjectMapper,
    private val avatars: AvatarRepository,
    private val clock: Clock,
) : JsonQueueConsumer<AvatarValidationPayload>(queueConfigs, objectMapper) {
    override fun execute(task: Task<AvatarValidationPayload>): TaskExecutionResult {
        val avatar = avatars.findById(task.payloadOrThrow.avatarId) ?: return TaskExecutionResult.finish()
        avatar.status = AvatarStatus.VALIDATED
        avatar.updatedAt = OffsetDateTime.now(clock)
        avatars.update(avatar)
        return TaskExecutionResult.finish()
    }
}
