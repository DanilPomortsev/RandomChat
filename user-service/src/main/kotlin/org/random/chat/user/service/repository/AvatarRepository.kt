package org.random.chat.user.service.repository

import org.jooq.Configuration
import org.random.chat.user.service.domain.AvatarStatus
import org.random.chat.user.service.jooq.tables.daos.UserAvatarsDao
import org.random.chat.user.service.jooq.tables.pojos.UserAvatars
import org.random.chat.user.service.jooq.tables.references.USER_AVATARS
import org.springframework.stereotype.Repository

@Repository
class AvatarRepository(
    configuration: Configuration,
) : UserAvatarsDao(configuration) {
    private val dsl = configuration.dsl()

    fun insert(userId: Long, url: String, status: AvatarStatus): UserAvatars {
        return dsl.insertInto(USER_AVATARS)
            .set(USER_AVATARS.USER_ID, userId)
            .set(USER_AVATARS.URL, url)
            .set(USER_AVATARS.STATUS, status)
            .returning()
            .fetchSingleInto(UserAvatars::class.java)
    }
}
