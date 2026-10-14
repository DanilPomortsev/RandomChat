package org.random.chat.user.service.domain

import org.jooq.impl.AbstractConverter

class UserStatusConverter : AbstractConverter<String, UserStatus>(String::class.java, UserStatus::class.java) {
    override fun from(databaseObject: String?): UserStatus? =
        databaseObject?.let { UserStatus.valueOf(it.uppercase()) }

    override fun to(userObject: UserStatus?): String? =
        userObject?.name?.lowercase()
}
