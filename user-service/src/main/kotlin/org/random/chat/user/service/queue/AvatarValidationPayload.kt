package org.random.chat.user.service.queue

import com.fasterxml.jackson.annotation.JsonIgnore
import org.random.chat.common.dbqueue.QueuePayload

data class AvatarValidationPayload(
    val avatarId: Long,
) : QueuePayload {
    @get:JsonIgnore
    override val queueName: String = QUEUE_NAME

    companion object {
        const val QUEUE_NAME = "avatar_validation"
    }
}
