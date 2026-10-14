package org.random.chat.user.service.service

import org.random.chat.common.dbqueue.DbQueueEnqueueService
import org.random.chat.user.service.domain.AvatarStatus
import org.random.chat.user.service.domain.UserStatus
import org.random.chat.user.service.model.UserInfo
import org.random.chat.user.service.queue.AvatarValidationPayload
import org.random.chat.user.service.repository.AvatarRepository
import org.random.chat.user.service.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class UserService(
    private val userRepository: UserRepository,
    private val avatarRepository: AvatarRepository,
    private val transactionTemplate: TransactionTemplate,
    private val dbQueueEnqueueService: DbQueueEnqueueService,
) {
    fun getById(id: Long): UserInfo? {
        return userRepository.findWithAvatars(id)
    }

    fun save(email: String, nickname: String, avatarUrl: String): Long {
        return transactionTemplate.execute {
            val user = userRepository.insert(email, nickname, UserStatus.ACTIVE)
            val userId = user.id!!
            val avatar = avatarRepository.insert(userId, avatarUrl, AvatarStatus.VALIDATION_IN_PROGRESS)
            dbQueueEnqueueService.enqueue(AvatarValidationPayload(avatar.id!!))
            userId
        }!!
    }
}
