package org.random.chat.user.service.repository

import org.jooq.Configuration
import org.jooq.impl.DSL.multiset
import org.jooq.impl.DSL.selectFrom
import org.random.chat.user.service.domain.AvatarStatus
import org.random.chat.user.service.domain.UserStatus
import org.random.chat.user.service.jooq.tables.daos.UsersDao
import org.random.chat.user.service.jooq.tables.pojos.UserAvatars
import org.random.chat.user.service.jooq.tables.pojos.Users
import org.random.chat.user.service.jooq.tables.references.USERS
import org.random.chat.user.service.jooq.tables.references.USER_AVATARS
import org.random.chat.user.service.model.UserInfo
import org.springframework.stereotype.Repository

@Repository
class UserRepository(
    configuration: Configuration,
) : UsersDao(configuration) {
    private val dsl = configuration.dsl()

    fun insert(email: String, nickname: String, status: UserStatus): Users {
        return dsl.insertInto(USERS)
            .set(USERS.EMAIL, email)
            .set(USERS.NICKNAME, nickname)
            .set(USERS.STATUS, status)
            .returning()
            .fetchSingleInto(Users::class.java)
    }

    fun findWithAvatars(id: Long): UserInfo? {
        val avatars = multiset(
            selectFrom(USER_AVATARS)
                .where(USER_AVATARS.USER_ID.eq(USERS.ID))
                .and(USER_AVATARS.STATUS.eq(AvatarStatus.VALIDATED))
                .orderBy(USER_AVATARS.ID.asc()),
        ).convertFrom { result -> result.into(UserAvatars::class.java) }

        return dsl.select(
            USERS.convertFrom { record -> record.into(Users::class.java) },
            avatars,
        )
            .from(USERS)
            .where(USERS.ID.eq(id))
            .fetchOne { record ->
                UserInfo(
                    user = record.value1(),
                    avatars = record.value2(),
                )
            }
    }
}
