package org.random.chat.common.dbqueue

import com.fasterxml.jackson.annotation.JsonIgnore

interface QueuePayload {
    @get:JsonIgnore
    val queueName: String
}
